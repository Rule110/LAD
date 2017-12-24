package content.similarity;

import peer.core.ActorMessage;
import peer.core.ActorMessageType;
import peer.core.UniversalId;
import peer.graph.weight.Weight;

/**
 * Alerts Contenter that a peer has viewed similar content
 *
 */
public class PeerSimilarViewAlert extends ActorMessage {
    private UniversalId similarViewPeerId;
    private Weight weightToGive;
    
    public PeerSimilarViewAlert(UniversalId similarViewPeerId, Weight weightToGive) {
        super(ActorMessageType.PeerSimilarViewAlert);
        this.similarViewPeerId = similarViewPeerId;
        this.weightToGive = weightToGive;
    }
    
    public UniversalId getSimilarViewPeerId() {
        return this.similarViewPeerId;
    }
    
    public Weight getWeightToGive() {
        return this.weightToGive;
    }
}