import adapter.Connector;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.ArgumentMatchers.anyString;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Connector.class)
public class ConnectorTest {

    @Test
    public void testInitiateMobileBankIdLogin() {

        Connector connector = PowerMockito.spy(new Connector());
        //PowerMockito.doReturn(0).when(connector, "postRequest", anyString(), anyString());
    }
}
