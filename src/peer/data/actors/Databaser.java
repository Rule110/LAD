package peer.data.actors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import content.frame.core.Content;
import content.frame.core.ContentFile;
import content.frame.messages.ContentFileExistenceRequest;
import content.frame.messages.ContentFileExistenceResponse;
import content.frame.messages.ContentFileRequest;
import content.frame.messages.ContentFileResponse;
import content.retrieve.messages.RetrievedContentFile;
import content.similarity.core.SimilarContentViewPeers;
import content.view.core.ContentView;
import content.view.core.ContentViews;
import content.view.messages.ContentViewAddition;
import filemanagement.core.FileConstants;
import filemanagement.fileretrieval.MediaFileSaver;
import filemanagement.filewrapper.FileUnwrapper;
import filemanagement.filewrapper.FileWrapper;
import peer.data.core.Constants;
import peer.data.messages.BackedUpContentViewHistoryRequest;
import peer.data.messages.BackedUpContentViewResponse;
import peer.data.messages.BackedUpPeerLinkResponse;
import peer.data.messages.BackedUpPeerLinksRequest;
import peer.data.messages.BackedUpSimilarContentViewPeersRequest;
import peer.data.messages.BackedUpSimilarContentViewPeersResponse;
import peer.data.messages.BackupContentViewInHistoryRequest;
import peer.data.messages.BackupPeerLinkRequest;
import peer.data.messages.BackupSimilarContentViewPeersRequest;
import peer.data.messages.LoadSavedContentFileRequest;
import peer.data.messages.LoadedContent;
import peer.data.messages.LocalSavedContentRequest;
import peer.data.messages.LocalSavedContentResponse;
import peer.data.messages.SaveContentFileRequest;
import peer.frame.actors.PeerToPeerActor;
import peer.frame.core.ActorPaths;
import peer.frame.exceptions.ImproperlyStoredContentFileException;
import peer.frame.exceptions.UnknownMessageException;
import peer.frame.messages.PeerToPeerActorInit;
import peer.graph.core.PeerWeightedLink;

/**
 * Database handler actor that stores data in Properties files
 *
 */
public class Databaser extends PeerToPeerActor {
    public Databaser() {
        this.checkDirExists(Constants.DATA_DIR);
    }
    
    /**
     * Check Data directory exists
     * @param dir
     */
    private void checkDirExists(String dir) {
        File directory = new File(dir);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }
    
    /**
     * Actor message processing
     */
    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof PeerToPeerActorInit) {
            PeerToPeerActorInit init = (PeerToPeerActorInit) message;
            this.initialisePeerToPeerActor(init);
            this.checkDirExists(Constants.getDataDir(super.peerId));
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
        else if (message instanceof BackupPeerLinkRequest) {
            BackupPeerLinkRequest request = (BackupPeerLinkRequest) message;
            this.processBackupPeerLinkRequest(request);
        }
        else if (message instanceof BackupSimilarContentViewPeersRequest) {
            BackupSimilarContentViewPeersRequest request = (BackupSimilarContentViewPeersRequest) message;
            this.processBackupSimilarContentViewPeersRequest(request);
        }
        else if (message instanceof BackedUpPeerLinksRequest) {
            BackedUpPeerLinksRequest request = (BackedUpPeerLinksRequest) message;
            this.processBackedUpPeerLinksRequest(request);
        }
        else if (message instanceof BackedUpSimilarContentViewPeersRequest) {
            BackedUpSimilarContentViewPeersRequest request = (BackedUpSimilarContentViewPeersRequest) message;
            this.processBackedUpSimilarContentViewPeersRequest(request);
        }
        else if (message instanceof BackupContentViewInHistoryRequest) {
            BackupContentViewInHistoryRequest request = (BackupContentViewInHistoryRequest) message;
            this.processBackupContentViewInHistoryRequest(request);
        }
        else if (message instanceof BackedUpContentViewHistoryRequest) {
            BackedUpContentViewHistoryRequest request = (BackedUpContentViewHistoryRequest) message;
            this.processBackedUpContentViewHistoryRequest(request);
        }
        else if (message instanceof SaveContentFileRequest) {
            SaveContentFileRequest request = (SaveContentFileRequest) message;
            this.processSaveContentFileRequest(request);
        }
        else if (message instanceof LocalSavedContentRequest) {
            LocalSavedContentRequest request = (LocalSavedContentRequest) message;
            this.processLocalSavedContentRequest(request);
        }
        else if (message instanceof LoadSavedContentFileRequest) {
            LoadSavedContentFileRequest request = (LoadSavedContentFileRequest) message;
            this.processLoadSavedContentFileRequest(request);
        }
        else {
            throw new UnknownMessageException();
        }
    }
    
    /**
     * Checks database to see if there is a file stored matching this Content object's description
     * @param request
     */
    protected void processContentFileExistenceRequest(ContentFileExistenceRequest request) {
        Content content = request.getContent();
        boolean exists = checkContentFileExists(content);
        ContentFileExistenceResponse response = new ContentFileExistenceResponse(content, exists);
        ActorRef requester = getSender();
        requester.tell(response, getSelf());
    }
    
    /**
     * Checks if a content file exists
     * @param content
     * @return
     */
    private boolean checkContentFileExists(Content content) {
        File file = new File(this.getFilePath(content));
        boolean exists = file.exists();
        return exists;
    }
    
    /**
     * Returns a content file to requesting actor if it exists in the database
     * @param request
     */
    protected void processContentFileRequest(ContentFileRequest request) throws IOException {
        Content content = request.getContent();
        ContentFile contentFile = getContentFile(content);
        ContentFileResponse response = new ContentFileResponse(contentFile);
        ActorRef requester = getSender();
        requester.tell(response, getSelf());
    }
    
    /**
     * Reads a Content File from disk
     * @param filepath
     * @return
     */
    private ContentFile getContentFile(Content content) throws IOException {
        Path path = Paths.get(this.getFilePath(content));
        byte[] appendedFilesArray = Files.readAllBytes(path);
        ContentFile contentFile = new ContentFile(content, appendedFilesArray);
        return contentFile;
    }
    
    /**
     * Get FilePath from File Name and File Format
     * @param fileName
     * @param fileFormat
     * @return
     * @throws IOException
     */
    private final String getFilePath(Content content) {
        String filePath = Constants.getDataDir(super.peerId) + content.getId() + Constants.CONTENT_FILE_EXTENSION;
        return filePath;
    }
    
    /**
     * Writes a retrieved content file to the database
     */
    protected void processRetrievedContentFile(RetrievedContentFile retrievedContentFile) throws IOException {
        ContentFile contentFile = retrievedContentFile.getContentFile();
        this.saveContentToManifest(contentFile.getContent());
        writeContentFile(contentFile);
    }
    
    /**
     * Writes a content file to disk
     * @param filepath
     * @param bytes
     * @throws IOException
     */
    private void writeContentFile(ContentFile contentFile) throws IOException {
        byte[] bytes = contentFile.getBytes();
        String filepath = this.getFilePath(contentFile.getContent());
        writeBytesToFile(filepath, bytes);
    }
    
    /**
     * Write bytes to file on disk
     * @param filepath
     * @param bytes
     * @throws IOException
     */
    private static void writeBytesToFile(String filepath, byte[] bytes) throws IOException {
        File file = new File(filepath);
        OutputStream out = new FileOutputStream(file, false);
        out.write(bytes, 0, bytes.length);
        out.close();
    }
    
    /**
     * Adds a Content View to the ContentViews header in the relevant Content File stored in the database
     * @param contentViewAddition
     */
    protected void processContentViewAddition(ContentViewAddition contentViewAddition) throws IOException {
        ContentView contentView = contentViewAddition.getContentView();
        Content content = contentView.getContent();
        ContentViews contentViews = getContentViews(content);
        contentViews.addContentView(contentView);
        setContentViews(contentViews);
    }
    
    /**
     * Gets the Content Views from the header of the content file stored on disk
     * @return
     */
    private ContentViews getContentViews(Content content) throws IOException {
        Path path = Paths.get(this.getFilePath(content));
        byte[] appendedFilesArray = Files.readAllBytes(path);
        byte[] headerArray = FileUnwrapper.extractHeaderArray(appendedFilesArray);
        String json = new String(headerArray);
        Gson gson = new Gson();
        ContentViews contentViews = gson.fromJson(json, ContentViews.class);
        if (!contentViews.getContent().equals(content)) throw new ImproperlyStoredContentFileException(contentViews.getContent(), content);
        return contentViews;
    }
    
    /**
     * Rewrites the header with the media segment of the content file back to disk
     * @param contentViews
     */
    private void setContentViews(ContentViews contentViews) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(contentViews);
        byte[] headerArray = json.getBytes();
        Content content = contentViews.getContent();
        Path path = Paths.get(this.getFilePath(content));
        byte[] appendedFilesArray = Files.readAllBytes(path);
        byte[] mediaArray = FileUnwrapper.extractFileArray(appendedFilesArray);
        appendedFilesArray = FileWrapper.mergeHeaderDataWithMediaFile(headerArray, mediaArray);
        writeBytesToFile(this.getFilePath(content), appendedFilesArray);
    }
    
    /**
     * Will back up a link this peer has to other peers
     * @param request
     */
    protected void processBackupPeerLinkRequest(BackupPeerLinkRequest request) {
        PeerWeightedLink peerWeightedLink = request.getPeerWeightedLink();
        Properties prop = new Properties();
        OutputStream output = null;
        String filename = Constants.getDataDir(super.peerId) + Constants.PEER_LINKS_FILENAME;
        Gson gson = new Gson();
        try {
            output = new FileOutputStream(filename, true);
            String key = peerWeightedLink.getLinkedPeerId().toString();
            String value = gson.toJson(peerWeightedLink);
            prop.setProperty(key, value);
            prop.store(output, null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Will back up a set of peers who watched the same content
     * @param request
     */
    protected void processBackupSimilarContentViewPeersRequest(BackupSimilarContentViewPeersRequest request) {
        SimilarContentViewPeers similarContentViewPeers = request.getSimilarContentViewPeers();
        Properties prop = new Properties();
        OutputStream output = null;
        String filename = Constants.getDataDir(super.peerId) + Constants.SIMILAR_CONTENT_VIEW_PEERS_FILENAME;
        Gson gson = new Gson();
        try {
            output = new FileOutputStream(filename, true);
            String key = similarContentViewPeers.getContent().getId();
            String value = gson.toJson(similarContentViewPeers);
            prop.setProperty(key, value);
            prop.store(output, null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Will back up a content view in the view history backup
     * @param request
     */
    protected void processBackupContentViewInHistoryRequest(BackupContentViewInHistoryRequest request) {
        ContentView contentView = request.getContentView();
        Properties prop = new Properties();
        OutputStream output = null;
        String filename = Constants.getDataDir(super.peerId) + Constants.CONTENT_VIEW_HISTORY_FILENAME;
        Gson gson = new Gson();
        try {
            output = new FileOutputStream(filename, true);
            String key = contentView.getContent().getId();
            String value = gson.toJson(contentView);
            prop.setProperty(key, value);
            prop.store(output, null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Will return all backed up peer links to the requester
     * @param request
     */
    protected void processBackedUpPeerLinksRequest(BackedUpPeerLinksRequest request) {
        String filename = Constants.getDataDir(super.peerId) + Constants.PEER_LINKS_FILENAME;
        File file = new File(filename);
        if (file.exists()) {
            ActorRef requester = getSender();
            Properties prop = new Properties();
            InputStream input = null;
            Gson gson = new Gson();
            try {
                input = new FileInputStream(filename);
                prop.load(input);
                Enumeration<?> e = prop.propertyNames();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    String value = prop.getProperty(key);
                    PeerWeightedLink peerWeightedLink = gson.fromJson(value, PeerWeightedLink.class);
                    BackedUpPeerLinkResponse response = new BackedUpPeerLinkResponse(peerWeightedLink);
                    requester.tell(response, getSelf());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    /**
     * Will return to the requester all backed up sets of peers who watched similar content
     * @param request
     */
    protected void processBackedUpSimilarContentViewPeersRequest(BackedUpSimilarContentViewPeersRequest request) {
        String filename = Constants.getDataDir(super.peerId) + Constants.SIMILAR_CONTENT_VIEW_PEERS_FILENAME;
        File file = new File(filename);
        if (file.exists()) {
            ActorRef requester = getSender();
            Properties prop = new Properties();
            InputStream input = null;
            Gson gson = new Gson();
            try {
                input = new FileInputStream(filename);
                prop.load(input);
                Enumeration<?> e = prop.propertyNames();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    String value = prop.getProperty(key);
                    SimilarContentViewPeers similarContentViewPeers = gson.fromJson(value, SimilarContentViewPeers.class);
                    BackedUpSimilarContentViewPeersResponse response = new BackedUpSimilarContentViewPeersResponse(similarContentViewPeers);
                    requester.tell(response, getSelf());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    /**
     * Will return to the requester all the backed up content views in the backed up view history
     * @param request
     */
    protected void processBackedUpContentViewHistoryRequest(BackedUpContentViewHistoryRequest request) {
        String filename = Constants.getDataDir(super.peerId) + Constants.CONTENT_VIEW_HISTORY_FILENAME;
        File file = new File(filename);
        if (file.exists()) {
            ActorRef requester = getSender();
            Properties prop = new Properties();
            InputStream input = null;
            Gson gson = new Gson();
            try {
                input = new FileInputStream(filename);
                prop.load(input);
                Enumeration<?> e = prop.propertyNames();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    String value = prop.getProperty(key);
                    ContentView contentView = gson.fromJson(value, ContentView.class);
                    BackedUpContentViewResponse response = new BackedUpContentViewResponse(contentView);
                    requester.tell(response, getSelf());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    /**
     * Saves Content File to data storage directory
     * @param request
     */
    protected void processSaveContentFileRequest(SaveContentFileRequest request) throws IOException {
        ContentFile contentFile = request.getContentFile();
        this.saveContentToManifest(contentFile.getContent());
        writeContentFile(contentFile);
    }
    
    /**
     * Iterates through manifest of saved content and sends content objects back in response
     * @param request
     * @throws IOException 
     */
    protected void processLocalSavedContentRequest(LocalSavedContentRequest request) throws IOException {
        ActorRef requester = getSender();
        LocalSavedContentResponse response = new LocalSavedContentResponse();
        
        File manifestFile = new File(Constants.getDataDir(super.peerId) + FileConstants.JSON_FILE_NAME);
        if (manifestFile.exists()) {
            String filesJSONString = new String (Files.readAllBytes(Paths.get(Constants.getDataDir(super.peerId) + FileConstants.JSON_FILE_NAME)));
    		JSONObject filesJSONObject = new JSONObject(filesJSONString);		
            JSONArray jsonArray = (JSONArray) filesJSONObject.get(FileConstants.JSON_FILES_KEY);
            
    		Gson gsonUtil = new Gson();
    		
    		for (int i = 0; i < jsonArray.length(); i++) {
    			response.add(gsonUtil.fromJson(jsonArray.get(i).toString(), Content.class));
    		}
        }
		
        requester.tell(response, getSelf());
    }
    
    /**
     * Saves content to manifest of local saved content
     * @param content
     * @throws IOException 
     */
    private void saveContentToManifest(Content content) throws IOException {
        File manifestFile = new File(Constants.getDataDir(peerId) + FileConstants.JSON_FILE_NAME);
        if (!manifestFile.exists()) {
            
            FileWriter jsonFile = new FileWriter(manifestFile, true);
            
            jsonFile.write(FileConstants.JSON_INIT);
            
            jsonFile.close();
        }
        
        String filesJSONString = new String (Files.readAllBytes(Paths.get(Constants.getDataDir(super.peerId) + FileConstants.JSON_FILE_NAME)));

        JSONObject filesJSONObject = new JSONObject(filesJSONString);
        Gson gsonUtil = new Gson();
        
        ((JSONArray) filesJSONObject.get(FileConstants.JSON_FILES_KEY))
            .put(new JSONObject(gsonUtil.toJson(content))
            );
        
        File jsonFile = new File(Constants.getDataDir(super.peerId) + FileConstants.JSON_FILE_NAME);
        
        try {
            FileWriter fileWriter = new FileWriter(jsonFile, false);
            fileWriter.write(filesJSONObject.toString());
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Databaser will send back the ContentFile requested as LoadedContent
     * @param request
     */
    protected void processLoadSavedContentFileRequest(LoadSavedContentFileRequest request) throws IOException {
        Content content = request.getContent();
        ContentFile contentFile = getContentFile(content);
        MediaFileSaver.writeMediaFile(content.getFileName(), content.getFileFormat(), FileUnwrapper.extractFileArray(contentFile.getBytes()));
        LoadedContent loadedContent = new LoadedContent(content);
        ActorSelection viewer = getContext().actorSelection(ActorPaths.getPathToViewer());
        viewer.tell(loadedContent, getSelf());
    }
}
