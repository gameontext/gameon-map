package net.wasdev.gameon.map.couchdb.auth;

import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import net.wasdev.gameon.map.models.ConnectionDetails;

/**
 * Tests for all the resource access policies and their factory
 */
public class ResourceAccessPolicyTest {

	private ResourceAccessPolicyFactory factory;
	public static final String TEST_SYSTEM_ID = "testSystemId";
	public static final String TEST_SWEEP_ID = "testSweepId";
	public static final String TEST_USER = "a test user of the system";
	public static final String TEST_OTHER_OWNER = "another owner of a resource that isn't the test user";

	@Before
	public void createFactory() {
		factory = new ResourceAccessPolicyFactory(TEST_SYSTEM_ID, TEST_SWEEP_ID);
	}

	@Test
	public void sweepCanAccessConnectionDetails() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(TEST_SWEEP_ID);
		assertThat(policy.isAuthorisedToView(null, ConnectionDetails.class), is(true));
	}

	@Test
	public void sweepCanNotAccessArbitraryClass() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(TEST_SWEEP_ID);
		assertThat(policy.isAuthorisedToView(null, String.class), is(false));
	}

	@Test
	public void systemCanAccessEverything() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(TEST_SYSTEM_ID);
		assertThat(policy.isAuthorisedToView(null, null), is(true));
		assertThat(policy.isAuthorisedToView(TEST_USER, ConnectionDetails.class), is(true));
		assertThat(policy.isAuthorisedToView(TEST_OTHER_OWNER, null), is(true));
	}

	@Test
	public void nullUserCanAccessNothing() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(null);
		assertThat(policy.isAuthorisedToView(null, null), is(false));
		assertThat(policy.isAuthorisedToView(TEST_USER, ConnectionDetails.class), is(false));
		assertThat(policy.isAuthorisedToView(TEST_OTHER_OWNER, null), is(false));
	}

	@Test
	public void userCanAccessOwnItems() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(TEST_USER);
		assertThat(policy.isAuthorisedToView(TEST_USER, null), is(true));
	}

	@Test
	public void userCanNotAccessOtherItems() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(TEST_USER);
		assertThat(policy.isAuthorisedToView(TEST_OTHER_OWNER, null), is(false));
	}

}