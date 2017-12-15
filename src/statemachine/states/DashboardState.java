package statemachine.states;

import gui.core.GUI;
import gui.core.SceneContainerStage;
import statemachine.core.StateMachine;
import statemachine.utils.StateName;

public class DashboardState extends State {
	private StateMachine stateMachine;
	private SceneContainerStage sceneContainerStage;
	private GUI gui;
	
	public DashboardState(StateMachine stateMachine, SceneContainerStage sceneContainerStage, GUI gui) {
		this.stateMachine = stateMachine;
		this.sceneContainerStage = sceneContainerStage;
		this.gui = gui;
	}

	public void execute(StateName param) {
		sceneContainerStage.changeScene(gui.getDashBoardScene());

		switch (param) {
			case ADD_FILE:
				addFile();
				break;
			case RETRIEVING_FILE:
				retrieveFile();
				break;
			case VIEWING_FILES:
				viewFiles();
				break;
			case REFRESH:
				refresh();
				break;
			default:
				break;
		}
	}
	
	private void addFile() {
		stateMachine.setCurrentState(StateName.ADD_FILE.toString());
		stateMachine.execute(null);
	}
	
	private void retrieveFile() {
		stateMachine.setCurrentState(StateName.RETRIEVE_FILE_QUERY.toString());
		stateMachine.execute(null);
	}
	
	private void viewFiles() {
		stateMachine.setCurrentState(StateName.VIEWING_FILES.toString());
		stateMachine.execute(null);
	}
	
	private void refresh() {
		// TODO
		// get recommendations
		// Update observable list with new data.
	}

}
