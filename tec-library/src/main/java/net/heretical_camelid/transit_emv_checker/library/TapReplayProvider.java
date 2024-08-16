package net.heretical_camelid.transit_emv_checker.library;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.parser.IProvider;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class TapReplayProvider extends MyProviderBase implements IProvider {

    final TapReplayConductor m_trc;
    final ArrayList<CommandAndResponse> m_commandsAndResponses;
    int m_stepIndex;

    public TapReplayProvider(TapReplayConductor trc) {
        m_trc = trc;
        m_commandsAndResponses = m_trc.getCommandsAndResponses();
        m_stepIndex = 0;
        setApduStore(trc.getAPDUObserver());
    }

    @Override
    protected byte[] implementationTransceive(byte[] pCommand, ByteBuffer receiveBuffer) throws CommunicationException {
        CommandAndResponse stepCarItem = m_commandsAndResponses.get(m_stepIndex);
        byte[] replayCommand = stepCarItem.rawCommand;

        // Allow the arbiter to control whether the command generated
        // by the framework is used unchanged, used with substitutions,
        // or replaced entirely by the command which was captured when
        // the replay data was generated.
        TapReplayArbiter.ReplayCompareOutcome commandOutcome = m_trc.getArbiter().compareAPDU(
            stepCarItem.stepName,
            TapReplayArbiter.APDUDirection.TERMINAL_TO_MEDIA,
            pCommand, replayCommand
        );

        switch(commandOutcome) {
            case ERROR_ABORT_NOW:
                throw new CommunicationException(
                    "Command mismatch at step " + stepCarItem.stepName
                );

            case OK_USE_CAPTURED_VALUE:
            case WARN_USE_CAPTURED_VALUE:
                // Replace the command passed in by the framework with the
                // one recorded during the preceding capture
                pCommand = replayCommand;
                break;

            default:
                // Use the command passed in by the framework
                // (which might have undergone some substitutions
                // during the arbiter/compare APDU operation)
        }

        byte[] replayResponse = stepCarItem.rawResponse;
        byte[] substitutedReplayResponse = null;
        if(replayResponse != null) {
            substitutedReplayResponse = Arrays.copyOf(
                replayResponse, replayResponse.length
            );
        }

        final byte[] selectedResponse;
        TapReplayArbiter.ReplayCompareOutcome responseOutcome = m_trc.getArbiter().compareAPDU(
            stepCarItem.stepName,
            TapReplayArbiter.APDUDirection.MEDIA_TO_TERMINAL,
            substitutedReplayResponse, replayResponse
        );
        switch(responseOutcome) {
            case ERROR_ABORT_NOW:
                throw new CommunicationException(
                    "Response mismatch at step " + stepCarItem.stepName
                );

            case OK_USE_CAPTURED_VALUE:
            case WARN_USE_CAPTURED_VALUE:
                // Replace the command passed in by the framework with the
                // one recorded during the preceding capture
                selectedResponse = replayResponse;
                break;

            default:
                // Use the command passed in by the framework
                // (which might have undergone some substitutions
                // during the arbiter/compare APDU operation)
                selectedResponse = substitutedReplayResponse;
        }
        LOGGER.info(String.format(
            "Replaying step %s: commandOutcome=%s responseOutcome=%s",
            stepCarItem.stepName, commandOutcome, responseOutcome
        ));

        m_stepIndex++;
        return selectedResponse;
    }

    @Override
    public byte[] getAt() {
        return new byte[0];
    }
}
