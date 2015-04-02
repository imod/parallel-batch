package hello;

import java.util.concurrent.FutureTask;

import org.springframework.batch.core.ItemProcessListener;

public class PersonItemProcessListener implements ItemProcessListener<Person, FutureTask<Person>> {

	@Override
	public void beforeProcess(Person item) {
		System.out.println("* PersonItemProcessListener.beforeProcess()");
	}

	@Override
	public void afterProcess(Person item, FutureTask<Person> result) {
		System.out.println("* PersonItemProcessListener.afterProcess()");
	}

	@Override
	public void onProcessError(Person item, Exception e) {
		// FIXME this method is never called when processing is done in parallel
		System.out.println("**** PersonItemProcessListener.onProcessError(), error on: " + e.getMessage());
	}

}
