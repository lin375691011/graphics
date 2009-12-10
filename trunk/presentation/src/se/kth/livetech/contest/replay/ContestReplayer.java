package se.kth.livetech.contest.replay;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import se.kth.livetech.contest.model.Attrs;
import se.kth.livetech.contest.model.AttrsUpdateEvent;
import se.kth.livetech.contest.model.impl.ContestImpl;
import se.kth.livetech.contest.model.impl.ContestUpdateEventImpl;

public class ContestReplayer extends ContestPlayer {
	
	private ContestImpl contest;
	private Set<AttrsUpdateEvent> updates;
	private long freezeTime = 0;
	private boolean paused = false;
	private Timer timer = null;
	
	public ContestReplayer() {
		contest = new ContestImpl();
		updates = Collections.synchronizedSet(new LinkedHashSet<AttrsUpdateEvent>());
	}
	
	public boolean isPaused() {
		return paused;
	}
	
	public void setPaused(boolean paused) {
		this.paused = paused;
		changed();
	}
	
	public long getFreezeTime() {
		return freezeTime;
	}
	
	public void setFreezeTime(long time) {
		if(time != freezeTime) {
			if(timer!=null) {
				timer.cancel();
				timer = null;
			}
			freezeTime = time;
			changed();
		}
	}
	
	public synchronized void attrsUpdated(AttrsUpdateEvent e) {
		updates.add(e);
		changed();
	}
	
	private void changed() {
		if(paused || updates.isEmpty()) {
			if(timer != null) {
				timer.cancel();
				timer = null;
			}
		} else {
			if(freezeTime==0) {
				UpdateTask updateTask = new UpdateTask();
				while(!updates.isEmpty())
					updateTask.run();
			} else if(timer == null) {
				timer = new Timer();
				timer.schedule(new UpdateTask(), freezeTime, freezeTime);
			}
		}
	}
	
	private void propagate(AttrsUpdateEvent e) {
		Attrs attrs = e.merge(contest);
		ContestImpl oldContest = contest;
		ContestImpl newContest = new ContestImpl(oldContest, attrs);
		contest = newContest;
		send(new ContestUpdateEventImpl(oldContest, attrs, newContest));
	}
	
	private class UpdateTask extends TimerTask {
		@Override
		public synchronized void run() {
			// TODO Find next task
			Iterator<AttrsUpdateEvent> it = updates.iterator();
			if(it.hasNext()) {
				// Propagate and erase ...
				AttrsUpdateEvent event = it.next();
				propagate(event);
				it.remove();
			}
			if(updates.isEmpty()) {
				timer.cancel();
				timer = null;
			}
		}	
	}
}