import { Component, type PropsWithChildren } from 'react';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '../shared/i18n/ErrorMessage';

function ErrorFallback({ onRetry }: Readonly<{ onRetry: () => void }>) {
  const { t } = useTranslation();
  return (
    <main>
      <ErrorMessage />
      <button type="button" onClick={onRetry}>{t('actions.retry')}</button>
    </main>
  );
}

export class AppErrorBoundary extends Component<PropsWithChildren, { failed: boolean }> {
  state = { failed: false };

  static getDerivedStateFromError() {
    return { failed: true };
  }

  render() {
    return this.state.failed
      ? <ErrorFallback onRetry={() => this.setState({ failed: false })} />
      : this.props.children;
  }
}
