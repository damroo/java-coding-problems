package elevator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ElevatorManagementSystem {

	public static void main(String[] args) throws InterruptedException {

		ElevatorController controller = new ElevatorController();
		
		IElevatorEnqueueStrategy elevatorEnqueueStrategy = new BasicEnqueueStrategy();

		Elevator e1 = new Elevator("elevator1", 10, elevatorEnqueueStrategy);// elevators can be divided for diff floors

		Elevator e2 = new Elevator("elevator2", 10, elevatorEnqueueStrategy);

		Elevator e3 = new Elevator("elevator3", 10, elevatorEnqueueStrategy);

		Elevator e4 = new Elevator("elevator4", 10, elevatorEnqueueStrategy);

		controller.addElevator(e1);
		controller.addElevator(e2);
		controller.addElevator(e3);
		controller.addElevator(e4);

		controller.startElevators();

		e1.pressElevatorButton(new ElevatorButton(7));

		Thread.sleep(5000);

		e1.pressElevatorButton(new ElevatorButton(9));

		Thread.sleep(5000);

		e1.pressElevatorButton(new ElevatorButton(1));
		
		Thread.sleep(5000);
		
		e1.pressElevatorButton(new ElevatorButton(-10));

	}
}

class ElevatorController {

	private List<Elevator> elevators = new ArrayList<>();

	private ExecutorService executors = Executors.newCachedThreadPool();

	public boolean addElevator(Elevator e) {
		return elevators.add(e);
	}

	public void startElevators() {

		if (elevators.size() == 0) {

			// throw exception

		}
		elevators.forEach(e -> {
			e.setRunning(true);
			executors.submit(e);
		});

	}

}

class Elevator implements Runnable {

	private final String elevatorId;

	private boolean running;

	private int currentFloor;

	private Direction direction;

	private final PriorityQueue<Integer> upQueue;

	private final PriorityQueue<Integer> downQueue;

	private IElevatorEnqueueStrategy elevatorEnqueueStrategy;

	public Elevator(String id, int totalFloors, IElevatorEnqueueStrategy elevatorEnqueueStrategy) {

		this.elevatorId = id;

		this.currentFloor = 0; // assuming each lift starts with 0 floor.

		this.upQueue = new PriorityQueue<>(totalFloors);

		this.downQueue = new PriorityQueue<>(totalFloors, Collections.reverseOrder());

		this.direction = Direction.GOINGUP;
		
		this.elevatorEnqueueStrategy = elevatorEnqueueStrategy;

	}

	public void pressElevatorButton(ElevatorButton button) {
		
		validateFloorPressed(button.getFloorPressed());
		
		elevatorEnqueueStrategy.addFloorsToService(currentFloor, upQueue, downQueue, button, direction);
	}
	
	private void validateFloorPressed(int floor) {
		
		if (floor < 0 || floor > 10) {
			throw new InvalidFloorRequestReceived.InvalidFloorRequstReceivedBuilder().elevatorId(elevatorId).floorNumber(floor).build();
		}
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public String getElevatorId() {
		return elevatorId;
	}

	public PriorityQueue<Integer> getUpQueue() {
		return upQueue;
	}

	public PriorityQueue<Integer> getDownQueue() {
		return downQueue;
	}

	public int getCurrentFloor() {
		return currentFloor;
	}

	public void setCurrentFloor(int currentFloor) {
		this.currentFloor = currentFloor;
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public void run() {

		System.out.println(elevatorId + " started.");

		while (isRunning()) {

			if (direction == Direction.GOINGUP)
				serveUpQueue();
			if (direction == Direction.GOINGDOWN)
				serveDownQueue();
			// add maintainence mode

		}

	}

	private void serveUpQueue() {

		while (upQueue.peek() != null) {

			int floorReached = upQueue.poll();

			currentFloor = floorReached;

			System.out.println(elevatorId + "  Going up Door open on  floor no:: " + floorReached);

			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		direction = Direction.GOINGDOWN;

	}

	private void serveDownQueue() {

		while (downQueue.peek() != null) {

			int floorReached = downQueue.poll();

			currentFloor = floorReached;

			System.out.println(elevatorId + "   Going down Door open on floor no:: " + floorReached);

		}
		direction = Direction.GOINGUP;

	}

}

enum Direction {

	GOINGUP, GOINGDOWN, MAINTAINENCE, NEUTRAL;
}

class ElevatorButton {

	private int floorPressed;

	public int getFloorPressed() {
		return floorPressed;
	}

	public void setFloorPressed(int floorPressed) {
		this.floorPressed = floorPressed;
	}

	public ElevatorButton(int floorPressed) {

		this.floorPressed = floorPressed;

	}
}

interface IElevatorEnqueueStrategy {
	
	public void addFloorsToService(int currentFloor, PriorityQueue<Integer> upQueue, PriorityQueue<Integer> downQueue, ElevatorButton button, Direction direction);

}

class BasicEnqueueStrategy implements IElevatorEnqueueStrategy {
	
	public void addFloorsToService(int currentFloor, PriorityQueue<Integer> upQueue, PriorityQueue<Integer> downQueue, ElevatorButton button, Direction direction) {
		
		if (button.getFloorPressed() == currentFloor) {
			System.out.println("Door already open on " + currentFloor + " floor.");
		}

		if (direction == Direction.GOINGUP && button.getFloorPressed() > currentFloor) {

			System.out.println("Elevator currently on floor ::" + currentFloor + " floor pressed going up "
					+ button.getFloorPressed());

			upQueue.add(button.getFloorPressed());

		}

		if (direction == Direction.GOINGDOWN && button.getFloorPressed() < currentFloor) {

			System.out.println("Elevator currently on floor ::" + currentFloor + " floor pressed going down "
					+ button.getFloorPressed());

			downQueue.add(button.getFloorPressed());

		}

		if (direction == Direction.GOINGUP && button.getFloorPressed() < currentFloor) { // assuming no reset happens
																							// upon reaching top or
																							// ground floor

			System.out.println("Elevator currently on floor ::" + currentFloor + ". Floor ::" + button.getFloorPressed()
					+ " is already served going up and will be served going down. ");

			downQueue.add(button.getFloorPressed());
		}

		if (direction == Direction.GOINGDOWN && button.getFloorPressed() > currentFloor) { // assuming no reset happens
																							// upon reaching top or
																							// ground floor

			System.out.println("Elevator currently on floor ::" + currentFloor + ". Floor ::" + button.getFloorPressed()
					+ " is already served going down and will be served going up. ");

			upQueue.add(button.getFloorPressed());
		}
		
	}

}

class InvalidFloorRequestReceived extends RuntimeException{
	
	private final int floorNumber;
	
	private final String elevatorId;
	
	private InvalidFloorRequestReceived(InvalidFloorRequstReceivedBuilder builder){
		this.floorNumber = builder.floorNumber;
		this.elevatorId = builder.elevatorId;
	}
	
	static class InvalidFloorRequstReceivedBuilder {
		
		private int floorNumber;
		
		private String elevatorId;
		
		public InvalidFloorRequstReceivedBuilder floorNumber(int floorNumber) {
			this.floorNumber = floorNumber;
			return this;
		}
		
		public InvalidFloorRequstReceivedBuilder elevatorId(String elevatorId) {
			this.elevatorId = elevatorId;
			return this;
		}
		
		public InvalidFloorRequestReceived build() {
			
			InvalidFloorRequestReceived invalidFloorRequestReceived = new InvalidFloorRequestReceived(this);
			
			return invalidFloorRequestReceived;
			
		}
		
	}

	@Override
	public String toString() {
		return "InvalidFloorRequestReceived [floorNumber=" + floorNumber + ", elevatorId=" + elevatorId + "]";
	}
}
