import com.sun.tools.attach.VirtualMachine;
import java.util.Arrays;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/** Local attach only: no remote JMX listener, credentials or HTTP test endpoint. */
class ReadMetric {
    public static void main(String[] args) throws Exception {
        var vm = VirtualMachine.attach("1");
        String address;
        try {
            address = vm.startLocalManagementAgent();
        } finally {
            vm.detach();
        }
        try (var connector = JMXConnectorFactory.connect(new JMXServiceURL(address))) {
            var server = connector.getMBeanServerConnection();
            var name = new ObjectName("org.springframework.boot:type=Endpoint,name=Metrics");
            var operation = Arrays.stream(server.getMBeanInfo(name).getOperations())
                    .filter(info -> info.getName().equals("metric")).findFirst().orElseThrow();
            var signature = Arrays.stream(operation.getSignature()).map(info -> info.getType()).toArray(String[]::new);
            Object tags = signature[1].equals("[Ljava.lang.String;") ? new String[0] : java.util.List.of();
            System.out.println(server.invoke(name, "metric", new Object[]{args[0], tags}, signature));
        }
    }
}
