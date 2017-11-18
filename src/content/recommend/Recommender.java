package content.recommend;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;

/**
 * Receives requests for Recommendations For User from the viewer
 * Delegates to a Peer Recommendation Aggregator that aggregates peer recommendations
 * Also exists on the other side of peer to peer communication
 * Receives requests for Peer Recommendations from a peer
 * Delegates to a History Recommendation Generator that generates a recommendation based on this peer's history
 * Receives back a Peer Recommendation to be sent back to the original requester as this peer's recommendation
 *
 */
public class Recommender extends UntypedActor {
    private int aggregatorCounter = 0;
    private int generatorCounter = 0;
    
    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof RecommendationsForUserRequest) {
            RecommendationsForUserRequest recommendationForUserRequest = 
        	    (RecommendationsForUserRequest) message;
            this.processRecommendationForUserRequest(recommendationForUserRequest);
        }
        else if (message instanceof RecommendationsForUser) {
            RecommendationsForUser recommendations = 
                    (RecommendationsForUser) message;
            this.processRecommendationsForUser(recommendations);
        }
        else if (message instanceof PeerRecommendationRequest) {
            PeerRecommendationRequest peerRecommendationRequest = 
                    (PeerRecommendationRequest) message;
            this.processPeerRecommendationRequest(peerRecommendationRequest);
        }
        else if (message instanceof PeerRecommendation) {
            PeerRecommendation peerRecommendation = 
                    (PeerRecommendation) message;
            this.processPeerRecommendation(peerRecommendation);
        }
        else {
            throw new RuntimeException("Unrecognised Message; Debug");
        }
    }
    
    /**
     * Creates an aggregator that will aggregate recommendations from peers
     * @param request
     */
    protected void processRecommendationForUserRequest(RecommendationsForUserRequest request) {
        ActorRef generator = getContext().actorOf(
                Props.create(PeerRecommendationAggregator.class), "peerAggregator_" + aggregatorCounter++);
        generator.tell(request, getSelf());
    }
    
    /**
     * Sends recommendations for user back to viewer
     * @param recommendations
     */
    protected void processRecommendationsForUser(RecommendationsForUser recommendations) {
        ActorSelection viewer = getContext().actorSelection("user/viewer");
        viewer.tell(recommendations, getSelf());
    }
    
    /**
     * Message asks for this Peer's recommendation
     * This peer will request its own View History to determine what recommendation to send back
     * @param recommendation
     */
    protected void processPeerRecommendationRequest(PeerRecommendationRequest peerRecommendationRequest) {
        ActorRef generator = getContext().actorOf(
                Props.create(HistoryRecommendationGenerator.class), "historyGenerator_" + generatorCounter++);
        
        generator.tell(peerRecommendationRequest, getSelf());
    }
    
    /**
     * Sends Peer Recommendation, generated by this peer, back to its original requester
     * @param peerRecommendation
     */
    protected void processPeerRecommendation(PeerRecommendation peerRecommendation) {
        ActorRef originalRequester = peerRecommendation.getOriginalRequester();
        originalRequester.tell(peerRecommendation, getSelf());
    }
}
