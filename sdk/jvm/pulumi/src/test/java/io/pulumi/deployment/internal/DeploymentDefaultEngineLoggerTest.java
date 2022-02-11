package io.pulumi.deployment.internal;

import io.pulumi.deployment.internal.DeploymentImpl.DefaultEngineLogger;
import org.junit.jupiter.api.Test;

import static io.pulumi.test.internal.assertj.PulumiConditions.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DeploymentDefaultEngineLoggerTest {

    @Test
    void testEngineLoggerDegradation() {
        var log = InMemoryLogger.getLogger("DeploymentDefaultEngineLoggerTest#testEngineLoggerDegradation");
        var log1 = new DefaultEngineLogger(
                log, () -> null, () -> null
        );
        log1.infoAsync("test degraded runner but working").join();

        var log2 = new DefaultEngineLogger(
                log, () -> mock(Runner.class), () -> null
        );
        log2.infoAsync("test degraded engine but working").join();

        // this test fails on CI with the last element missing without the 'join()'s
        assertThat(log.getMessages()).hasSize(4);
        assertThat(log.getMessages()).haveAtLeastOne(containsString("Degraded functionality [DefaultEngineLogger]: async logging is unavailable because of no Runner"));
        assertThat(log.getMessages()).haveAtLeastOne(containsString("Degraded functionality [DefaultEngineLogger]: async logging is unavailable because of no Engine"));
    }
}
