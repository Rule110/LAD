package gui.core;

import content.content.Content;
import gui.panes.DashBoardPane;
import gui.panes.FileRetrievalQueryPane;
import gui.panes.MyFilesPane;
import gui.panes.RatingPane;
import gui.panes.SetupPane;
import gui.panes.WaitingPane;
import gui.scenes.DashBoardScene;
import gui.scenes.FileRetrievalQueryScene;
import gui.scenes.FileRetrievalScene;
import gui.scenes.MyFilesScene;
import gui.scenes.RatingScene;
import gui.scenes.RetrieveRecommendationsScene;
import gui.scenes.SetupScene;
import gui.utilities.GUIText;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ProgressBar;

public class GUI {
	private DashBoardScene dashBoardScene;
	private FileRetrievalQueryScene fileRetrievalQueryScene;
	private FileRetrievalScene fileRetrievalScene;
	private MyFilesScene myFilesScene;
	private RatingScene ratingScene;
	private RetrieveRecommendationsScene retrieveRecommendationsScene;
	private SetupScene setupScene;
	private DashBoardPane dashBoardPane;
	private FileRetrievalQueryPane fileRetrievalQueryPane;
	private WaitingPane fileRetrievalPane;
	private MyFilesPane myFilesPane;
	private RatingPane ratingPane;
	private WaitingPane retrieveRecommendationsPane;
	private SetupPane setupPane;
	
	public GUI(SceneContainerStage containerStage) {
		ObservableList<Content> data = FXCollections.observableArrayList();
		data.addAll(new Content("234134", "Akira", ".mp4", "https://speed.hetzner.de/100MB.bin"), 
				new Content("234134as", "Shakira - Shewolf", ".mp4", "http://francislawlor.com/modules.html"), 
				new Content("31234da", "My Chemical Romance - I am Sad", ".mp3", "http://francislawlor.com/modules.html"));

		this.dashBoardPane = new DashBoardPane(data);
		this.fileRetrievalQueryPane = new FileRetrievalQueryPane();
		this.fileRetrievalPane = new WaitingPane(GUIText.RETRIEVING_FILE);
		this.myFilesPane = new MyFilesPane(null);
		this.ratingPane = new RatingPane();
		this.retrieveRecommendationsPane = new WaitingPane(GUIText.FINDING_RECOMMENDATIONS);
		this.setupPane = new SetupPane(containerStage);
		this.dashBoardScene = new DashBoardScene(this.dashBoardPane);
		this.fileRetrievalQueryScene = new FileRetrievalQueryScene(this.fileRetrievalQueryPane);
		this.fileRetrievalScene = new FileRetrievalScene(this.fileRetrievalPane);
		this.myFilesScene = new MyFilesScene(this.myFilesPane);
		this.ratingScene = new RatingScene(this.ratingPane);
		this.retrieveRecommendationsScene = new RetrieveRecommendationsScene(this.retrieveRecommendationsPane);
		this.setupScene = new SetupScene(this.setupPane);
	}
	
	public DashBoardScene getDashBoardScene() {
		return this.dashBoardScene;
	}
	
	public FileRetrievalScene getFileRetrievalScene() {
		return this.fileRetrievalScene;
	}
	
	public FileRetrievalQueryScene getFileRetrievalQueryScene() {
		return this.fileRetrievalQueryScene;
	}

	public MyFilesScene getMyFilesScene() {
		return this.myFilesScene;
	}

	public RatingScene getRatingScene() {
		return this.ratingScene;
	}

	public RetrieveRecommendationsScene getRetrieveRecommendationsScene() {
		return this.retrieveRecommendationsScene;
	}

	public SetupScene getSetupScene() {
		return this.setupScene;
	}
}
