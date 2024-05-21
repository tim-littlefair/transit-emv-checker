package net.heretical_camelid.transit_emv_checker.library;

import java.util.Arrays;

/**
 * The TapReplayArbiter is an object which is accessed
 * by the provider on each replay step, and is used
 * on both the command and response APDUs to compare
 * them with the corresponding APDUs from the capture
 * data, and determine whether the replay is still
 * on track (given that command APDUs can include
 * variable values like current date, time and
 * unpredictable number, and that response APDUs
 * may need to be adjusted to reflect these).
 */
public class TapReplayArbiter {

    public enum APDUDirection {
        TERMINAL_TO_MEDIA,
        MEDIA_TO_TERMINAL
    };

    public enum ReplayCompareOutcome {
        OK_NO_DIFFERENCE,
        OK_USE_CURRENT_VALUE,
        OK_USE_CURRENT_VALUE_WITH_SUBSTITUTIONS,
        OK_USE_CAPTURED_VALUE,
        WARN_USE_CURRENT_VALUE_AS_IS,
        WARN_USE_CURRENT_VALUE_WITH_SUBSTITUTIONS,
        WARN_USE_CAPTURED_VALUE,
        ERROR_ABORT_NOW
    }

    final private ReplayCompareOutcome m_leastBadUnacceptableOutcome;

    public TapReplayArbiter() {
        m_leastBadUnacceptableOutcome = ReplayCompareOutcome.WARN_USE_CAPTURED_VALUE;
    }

    public ReplayCompareOutcome compareAPDU(
        String stepName,
        APDUDirection apduDirection,
        byte[] currentValue,
        byte[] capturedValue
    ) {
        if (Arrays.equals(currentValue, capturedValue)) {
            return ReplayCompareOutcome.OK_NO_DIFFERENCE;
        } else {
            // TODO: Add in support for substitutions
            if (currentValue.length == capturedValue.length) {
                return ReplayCompareOutcome.OK_USE_CURRENT_VALUE;
            } else {
                return ReplayCompareOutcome.ERROR_ABORT_NOW;
            }
        }
    }
}
