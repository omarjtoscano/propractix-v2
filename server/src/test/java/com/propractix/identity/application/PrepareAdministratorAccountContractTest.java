package com.propractix.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.propractix.identity.application.port.in.PrepareAdministratorAccount;
import com.propractix.identity.application.port.in.PrepareAdministratorAccount.Outcome;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Contract shape only. No provisioning/recovery ceremony is implemented or accredited. */
class PrepareAdministratorAccountContractTest {
    @ParameterizedTest
    @EnumSource(Outcome.class)
    void resultContainsOnlyIdsAndTypedOutcomeWithConsistentPresence(Outcome outcome) {
        UUID onboarding = UUID.randomUUID();
        boolean prepared = outcome == Outcome.NEW_ACCOUNT_PREPARED
                || outcome == Outcome.PENDING_ACCOUNT_RECOVERY_REQUIRED
                || outcome == Outcome.ACCOUNT_CONTINUATION_REQUIRED;
        Optional<UUID> account = prepared ? Optional.of(UUID.randomUUID()) : Optional.empty();
        // A double can supply any proposed result; production E5 remains unauthorized.
        PrepareAdministratorAccount doublePort = command -> new PrepareAdministratorAccount.Result(
                command.onboardingId(), outcome, account);
        var result = doublePort.prepare(new PrepareAdministratorAccount.Command(onboarding,
                "Synthetic+Admin@company.example", "Synthetic Administrator", "en", UUID.randomUUID()));
        assertThat(result.onboardingId()).isEqualTo(onboarding);
        assertThat(result.userId()).isEqualTo(account);
        assertThat(result.outcome()).isEqualTo(outcome);
        assertThat(Arrays.stream(result.getClass().getRecordComponents()).map(c -> c.getName()))
                .containsExactly("onboardingId", "outcome", "userId");
        Optional<UUID> inconsistent = prepared ? Optional.empty() : Optional.of(UUID.randomUUID());
        assertThatThrownBy(() -> new PrepareAdministratorAccount.Result(onboarding, outcome, inconsistent))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transientCommandIsRedactedAndHasNoCredentialOrTenantFields() {
        var command = new PrepareAdministratorAccount.Command(UUID.randomUUID(), "synthetic@company.example",
                "Synthetic Administrator", "en", UUID.randomUUID());
        assertThat(command.toString()).isEqualTo("PrepareAdministratorAccount.Command[redacted]");
        assertThat(Arrays.stream(command.getClass().getRecordComponents()).map(c -> c.getName()))
                .containsExactly("onboardingId", "canonicalEmail", "administratorName", "locale", "correlationId");
    }
}
