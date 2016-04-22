package net.wasdev.gameon.map.couchdb.auth;

import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

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
		assertThat(policy, canViewResource(ConnectionDetails.class));
	}

	@Test
	public void sweepCanNotAccessArbitraryClass() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(TEST_SWEEP_ID);
		assertThat(policy, not(canViewResource(String.class)));
	}

	@Test
	public void systemCanAccessEverything() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(TEST_SYSTEM_ID);
		assertThat(policy, canViewResourceOwnedBy(null));
		assertThat(policy, canViewResourceOwnedBy(TEST_USER));
		assertThat(policy, canViewResourceOwnedBy(TEST_OTHER_OWNER));
	}

	@Test
	public void nullUserCanAccessNothing() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(null);
		assertThat(policy, not(canViewResourceOwnedBy(null)));
		assertThat(policy, not(canViewResourceOwnedBy(TEST_USER)));
		assertThat(policy, not(canViewResourceOwnedBy(TEST_OTHER_OWNER)));
	}

	@Test
	public void userCanAccessOwnItems() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(TEST_USER);
		assertThat(policy, canViewResourceOwnedBy(TEST_USER));
	}

	@Test
	public void userCanNotAccessOtherItems() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(TEST_USER);
		assertThat(policy, not(canViewResourceOwnedBy(TEST_OTHER_OWNER)));
	}
	
	private static Matcher<ResourceAccessPolicy> canViewResourceOwnedBy(String owner) {
		return new AuthorisedToViewMatcher(owner, null);
	}
	
	private static Matcher<ResourceAccessPolicy> canViewResource(Class<?> resource) {
		return new AuthorisedToViewMatcher(null, resource);
	}
	
	private static class AuthorisedToViewMatcher extends BaseMatcher<ResourceAccessPolicy> {

		private final String owner;
		private final Class<?> resourceType;
		
		public AuthorisedToViewMatcher(String owner, Class<?> resourceType) {
			this.owner = owner;
			this.resourceType = resourceType;
		}

		@Override
		public boolean matches(Object item) {
			ResourceAccessPolicy policy = (ResourceAccessPolicy) item;
			return policy.isAuthorisedToView(owner, resourceType);
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("should be able to view resource ")
						.appendValue(resourceType)
						.appendText(" owned by ")
						.appendText(owner);
		}
		
	}

}