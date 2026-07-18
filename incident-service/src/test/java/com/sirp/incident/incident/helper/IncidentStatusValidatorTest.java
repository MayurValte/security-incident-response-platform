package com.sirp.incident.incident.helper;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sirp.incident.exception.InvalidStatusTransitionException;
import com.sirp.incident.incident.enums.IncidentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class IncidentStatusValidatorTest {

    private final IncidentStatusValidator validator = new IncidentStatusValidator();

    @Test
    void allowsOpenToAcknowledged() {
        assertThatCode(() -> validator.validate(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED))
            .doesNotThrowAnyException();
    }

    @Test
    void allowsAcknowledgedToInProgress() {
        assertThatCode(() -> validator.validate(IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS))
            .doesNotThrowAnyException();
    }

    @Test
    void allowsInProgressToResolved() {
        assertThatCode(() -> validator.validate(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED))
            .doesNotThrowAnyException();
    }

    @Test
    void allowsResolvedToClosed() {
        assertThatCode(() -> validator.validate(IncidentStatus.RESOLVED, IncidentStatus.CLOSED))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = "ACKNOWLEDGED", mode = EnumSource.Mode.EXCLUDE)
    void rejectsAnyTargetFromOpenOtherThanAcknowledged(IncidentStatus target) {
        assertThatThrownBy(() -> validator.validate(IncidentStatus.OPEN, target))
            .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = "IN_PROGRESS", mode = EnumSource.Mode.EXCLUDE)
    void rejectsAnyTargetFromAcknowledgedOtherThanInProgress(IncidentStatus target) {
        assertThatThrownBy(() -> validator.validate(IncidentStatus.ACKNOWLEDGED, target))
            .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = "RESOLVED", mode = EnumSource.Mode.EXCLUDE)
    void rejectsAnyTargetFromInProgressOtherThanResolved(IncidentStatus target) {
        assertThatThrownBy(() -> validator.validate(IncidentStatus.IN_PROGRESS, target))
            .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = "CLOSED", mode = EnumSource.Mode.EXCLUDE)
    void rejectsAnyTargetFromResolvedOtherThanClosed(IncidentStatus target) {
        assertThatThrownBy(() -> validator.validate(IncidentStatus.RESOLVED, target))
            .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @ParameterizedTest
    @EnumSource(IncidentStatus.class)
    void closedIsAlwaysTerminalRegardlessOfTarget(IncidentStatus target) {
        assertThatThrownBy(() -> validator.validate(IncidentStatus.CLOSED, target))
            .isInstanceOf(InvalidStatusTransitionException.class);
    }

    /**
     * ON_HOLD has no case in the validator's switch at all - not a typo in
     * this test. A switch *statement* (unlike a switch expression) doesn't
     * require exhaustiveness, so validate(ON_HOLD, ...) falls through doing
     * nothing rather than throwing. Documenting the real current behavior
     * rather than the behavior one might assume from the status enum name.
     */
    @ParameterizedTest
    @EnumSource(IncidentStatus.class)
    void onHoldIsNotHandledByTheValidatorAndNeverThrows(IncidentStatus target) {
        assertThatCode(() -> validator.validate(IncidentStatus.ON_HOLD, target)).doesNotThrowAnyException();
    }
}
