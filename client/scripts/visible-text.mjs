import { parseSync } from 'rolldown/utils';

const presentationAttributes = new Set([
  'title', 'alt', 'placeholder', 'aria-label', 'aria-description', 'aria-roledescription',
  'aria-valuetext', 'label', 'helperText', 'errorMessage', 'message',
]);

function walk(node, visit) {
  if (node === null || typeof node !== 'object') return;
  if (Array.isArray(node)) return node.forEach((child) => walk(child, visit));
  if (typeof node.type === 'string') visit(node);
  for (const child of Object.values(node)) {
    if (child !== null && typeof child === 'object') walk(child, visit);
  }
}

/** Source-level guard; it does not attempt cross-file data-flow analysis. */
export function visibleTextViolations(source, filename, knownKeys) {
  const parsed = parseSync(filename, source);
  if (parsed.errors.length > 0) throw new Error(`i18n_source_parse_failed:${filename}`);
  const issues = [];
  const report = (node, code) => {
    const line = source.slice(0, node.start).split('\n').length;
    issues.push(`${code}:${filename}:${line}`);
  };
  const isTranslation = (node) => node.type === 'CallExpression'
    && (node.callee.type === 'Identifier' && node.callee.name === 't'
      || node.callee.type === 'MemberExpression' && node.callee.property.name === 't');

  const checkPresentation = (node) => {
    if (node === null || node === undefined) return;
    if (node.type === 'Literal' && typeof node.value === 'string' && node.value.trim() !== '') {
      report(node, 'visible_text_must_use_i18n');
    } else if (node.type === 'TemplateLiteral') {
      if (node.quasis.some((part) => part.value.cooked?.trim())) report(node, 'visible_text_must_use_i18n');
      node.expressions.forEach(checkPresentation);
    } else if (node.type === 'ConditionalExpression') {
      checkPresentation(node.consequent);
      checkPresentation(node.alternate);
    } else if (node.type === 'BinaryExpression' || node.type === 'LogicalExpression') {
      if (node.operator !== '&&') checkPresentation(node.left);
      checkPresentation(node.right);
    } else if (node.type === 'JSXExpressionContainer' || node.type === 'ChainExpression') {
      checkPresentation(node.expression);
    } else if (node.type === 'TSAsExpression' || node.type === 'TSNonNullExpression') {
      checkPresentation(node.expression);
    }
    // Technical exception/Problem Details prose must not become UI copy.
    walk(node, (child) => {
      if (child.type !== 'MemberExpression') return;
      const property = child.computed ? child.property.value : child.property.name;
      const receiver = source.slice(child.object.start, child.object.end);
      if (['message', 'detail', 'title'].includes(property)
          && /(error|problem|exception|failure|violation|\berr\b)/iu.test(receiver)) {
        report(child, 'raw_error_text_must_use_code');
      }
    });
  };

  walk(parsed.program, (node) => {
    if (node.type === 'JSXText' && node.value.trim() !== '') report(node, 'visible_text_must_use_i18n');
    if (node.type === 'JSXElement' || node.type === 'JSXFragment') {
      node.children.filter((child) => child.type === 'JSXExpressionContainer').forEach(checkPresentation);
    }
    if (node.type === 'JSXAttribute' && presentationAttributes.has(node.name.name)) {
      checkPresentation(node.value);
    }
    if (isTranslation(node)) {
      const key = node.arguments[0];
      if (key?.type === 'Literal' && typeof key.value === 'string' && !knownKeys.has(key.value)) {
        report(key, 'unknown_i18n_key');
      }
      const options = node.arguments[1];
      if (options?.type === 'Literal' && typeof options.value === 'string') {
        report(options, 'i18n_default_text_must_use_catalog');
      }
      if (options?.type === 'ObjectExpression') {
        for (const property of options.properties) {
          if ((property.key?.name ?? property.key?.value) === 'defaultValue') {
            report(property, 'i18n_default_text_must_use_catalog');
          }
        }
      }
    }
  });
  return [...new Set(issues)];
}
