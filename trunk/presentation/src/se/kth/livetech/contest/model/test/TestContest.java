package se.kth.livetech.contest.model.test;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArraySet;

import se.kth.livetech.contest.graphics.ICPCImages;
import se.kth.livetech.contest.model.Attrs;
import se.kth.livetech.contest.model.AttrsUpdateEvent;
import se.kth.livetech.contest.model.AttrsUpdateListener;
import se.kth.livetech.contest.model.AttrsUpdater;
import se.kth.livetech.contest.model.Contest;
import se.kth.livetech.contest.model.Language;
import se.kth.livetech.contest.model.Problem;
import se.kth.livetech.contest.model.Run;
import se.kth.livetech.contest.model.Team;
import se.kth.livetech.contest.model.Testcase;
import se.kth.livetech.contest.model.impl.AttrsUpdateEventImpl;
import se.kth.livetech.contest.model.impl.ContestImpl;
import se.kth.livetech.contest.model.impl.LanguageImpl;
import se.kth.livetech.contest.model.impl.ProblemImpl;
import se.kth.livetech.contest.model.impl.RunImpl;
import se.kth.livetech.contest.model.impl.TeamImpl;
import se.kth.livetech.contest.model.impl.TestcaseImpl;

public class TestContest implements AttrsUpdater {
	public static Team testTeam(int id, String name, String univ, String nat) {
		return testTeam(id, name + '(' + univ + '/' + nat + ')');
	}
	
	private static Map<String, String> entityMap(int id, String name) {
		Map<String, String> m = new TreeMap<String, String>();
		m.put("id", "" + id);
		m.put("name", name);
		return m;
	}

	public static Team testTeam(int id, String name) {
		return new TeamImpl(entityMap(id, name));
	}

	public static Problem testProblem(int id, String name) {
		return new ProblemImpl(entityMap(id, name));
	}

	public static Language testLanguage(int id, String name) {
		return new LanguageImpl(entityMap(id, name));
	}

	public static ContestImpl testContest(int teams, int problems) {
		ContestImpl c = new ContestImpl();
		c = new ContestImpl(c, testLanguage(0, "c"));
		c = new ContestImpl(c, testLanguage(1, "c++"));
		c = new ContestImpl(c, testLanguage(2, "java"));
		for (int problem = 0; problem < problems; ++problem) {
			Problem p = testProblem(problem, "" + (char) ('A' + problem));
			c = new ContestImpl(c, p);
		}
		for (int team = 0; team < teams; ++team) {
			String[] COUNTRY_CODES = ICPCImages.COUNTRY_CODES;
			Team t = testTeam(team, "Team " + team + " (University " + team + "/"
					+ COUNTRY_CODES[team % COUNTRY_CODES.length] + ")");
			c = new ContestImpl(c, t);
		}
		return c;
	}

	public static Run testRun(int id, int team, int problem, int time) {
		Map<String, String> m = new TreeMap<String, String>();
		m.put("id", "" + id);
		m.put("status", "fresh");
		m.put("team", "" + team);
		m.put("problem", "" + problem);
		m.put("time", "" + time);
		return new RunImpl(m);
	}
	
	public static Testcase testCase(int id, int i, int n, boolean judged, boolean solved) {
		Map<String, String> m = new TreeMap<String, String>();
		m.put("run-id", "" + id);
		m.put("i", "" + i);
		m.put("n", "" + n);
		m.put("judged", "" + judged);
		m.put("solved", "" + solved);
		return new TestcaseImpl(m);
	}

	public static Run testUpdate(Contest c, int id, boolean solved) {
		RunImpl r = (RunImpl) c.getRun(id);
		Map<String, String> m = new TreeMap<String, String>();
		for (String p : r.getProperties())
			m.put(p, r.getProperty(p));
		m.put("status", "done");
		m.put("judged", "" + true);
		m.put("solved", "" + solved);
		m.put("penalty", "" + !solved);
		return new RunImpl(m);
	}

	ContestImpl c;
	int run = 0;

	public TestContest(int teams, int problems) {
		c = testContest(teams, problems);
	}

	private synchronized void update(Attrs a) {
		c = new ContestImpl(c, a);
		AttrsUpdateEvent e = new AttrsUpdateEventImpl(System.currentTimeMillis(), a);
		for (AttrsUpdateListener listener : listeners) {
			listener.attrsUpdated(e);
		}
	}

	public int submit(int team, int problem, int time) {
		int id = run++;
		update(testRun(id, team, problem, time));
		return id;
	}
	
	public void solve(int id) {
		update(testUpdate(c, id, true));
	}

	public void fail(int id) {
		update(testUpdate(c, id, false));
	}
	
	public void testcase(int id, int i, int n, boolean judged, boolean solved) {
		update(testCase(id, i, n, judged, solved));
	}

	public Contest getContest() {
		return c;
	}

	private Set<AttrsUpdateListener> listeners = new CopyOnWriteArraySet<AttrsUpdateListener>();

	public synchronized void addAttrsUpdateListener(AttrsUpdateListener listener) {
		listeners.add(listener);
	}

	public synchronized void removeAttrsUpdateListener(AttrsUpdateListener listener) {
		listeners.remove(listener);
	}
}
