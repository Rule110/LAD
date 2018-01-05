package peer.data;

import java.io.IOException;

import content.core.Content;
import content.core.ContentFile;
import content.core.ContentFileExistenceRequest;
import content.core.ContentFileRequest;
import content.retrieve.RetrievedContentFile;
import content.view.ContentViewAddition;
import filemanagement.fileretrieval.FileManager;
import peer.core.PeerToPeerActor;
import peer.core.PeerToPeerActorInit;
import peer.core.xcept.UnknownMessageException;

/**
 * Actor that handles the database
 *
 */
@SuppressWarnings("unused")
public class Databaser extends PeerToPeerActor {
    private Database db;
    
    /**
     * Actor message processing
     */
    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof PeerToPeerActorInit) {
            PeerToPeerActorInit init = (PeerToPeerActorInit) message;
            this.initialisePeerToPeerActor(init);
        }
        else if (message instanceof DatabaserInit) {
            DatabaserInit init = (DatabaserInit) message;
            this.initialiseDatabaser(init.getDatabase());
        }
        else if (message instanceof ContentFileExistenceRequest) {
            ContentFileExistenceRequest request = (ContentFileExistenceRequest) message;
            this.processContentFileExistenceRequest(request);
        }
        else if (message instanceof ContentFileRequest) {
            ContentFileRequest request = (ContentFileRequest) message;
            this.processContentFileRequest(request);
        }
        else if (message instanceof RetrievedContentFile) {
            RetrievedContentFile retrievedContentFile = (RetrievedContentFile) message;
            this.processRetrievedContentFile(retrievedContentFile);
        }
        else if (message instanceof ContentViewAddition) {
            ContentViewAddition addition = (ContentViewAddition) message;
            this.processContentViewAddition(addition);
        }
        else {
            throw new UnknownMessageException();
        }
    }
    
    /**
     * Initialise the Database in the databaser actor's state
     * @param db
     */
    protected void initialiseDatabaser(Database db) {
        this.db = db;
    }
    
    /**
     * Checks database to see if there is a file stored matching this Content object's description
     * @param request
     */
    protected void processContentFileExistenceRequest(ContentFileExistenceRequest request) {
        Content content = request.getContent();
        
    }
    
    /**
     * Returns a content file to requesting actor if it exists in the database
     * @param request
     */
    protected void processContentFileRequest(ContentFileRequest request) {
        Content content = request.getContent();
        String filename = content.getFileName();
        
    }
    
    /**
     * Writes a retrieved content file to the database
     */
    protected void processRetrievedContentFile(RetrievedContentFile retrievedContentFile) throws IOException {
        ContentFile contentFile = retrievedContentFile.getContentFile();
        FileManager.writeContentFile(contentFile);
    }
    
    /**
     * Adds a Content View to the ContentViews header in the relevant Content File stored in the database
     * @param contentViewAddition
     */
    protected void processContentViewAddition(ContentViewAddition contentViewAddition) {
        
    }
}
