package net.heretical_camelid.transit_emv_checker.library;

import java.util.Arrays;

public class TapReplayArbiter {

    public enum APDUDirection {
        MOBILE_TO_MEDIA,
        MEDIA_TO_MOBILE
    };

    public enum ReplayCompareOutcome {
        OK_NO_DIFFERENCE,
        OK_USE_CURRENT_VALUE,
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
            if (currentValue.length == capturedValue.length) {
                return ReplayCompareOutcome.OK_USE_CURRENT_VALUE;
            } else {
                return ReplayCompareOutcome.ERROR_ABORT_NOW;
            }
        }
    }
}
