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
		assertThat(policy, is(ableToViewResource(ConnectionDetails.class)));
	}

	@Test
	public void sweepCanNotAccessArbitraryClass() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(TEST_SWEEP_ID);
		assertThat(policy, not(ableToViewResource(String.class)));
	}

	@Test
	public void systemCanAccessEverything() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(TEST_SYSTEM_ID);
		assertThat(policy, is(ableToViewResourceOwnedBy(null)));
		assertThat(policy, is(ableToViewResourceOwnedBy(TEST_USER)));
		assertThat(policy, is(ableToViewResourceOwnedBy(TEST_OTHER_OWNER)));
	}

	@Test
	public void nullUserCanAccessNothing() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(null);
		assertThat(policy, not(ableToViewResourceOwnedBy(null)));
		assertThat(policy, not(ableToViewResourceOwnedBy(TEST_USER)));
		assertThat(policy, not(ableToViewResourceOwnedBy(TEST_OTHER_OWNER)));
	}

	@Test
	public void userCanAccessOwnItems() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(TEST_USER);
		assertThat(policy, is(ableToViewResourceOwnedBy(TEST_USER)));
	}

	@Test
	public void userCanNotAccessOtherItems() {
		ResourceAccessPolicy policy = factory.createPolicyForUser(TEST_USER);
		assertThat(policy, not(ableToViewResourceOwnedBy(TEST_OTHER_OWNER)));
	}
	
	private static Matcher<ResourceAccessPolicy> ableToViewResourceOwnedBy(String owner) {
		return new AuthorisedToViewMatcher(owner, null);
	}
	
	private static Matcher<ResourceAccessPolicy> ableToViewResource(Class<?> resource) {
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
			description.appendText("able to view resource of type ")
						.appendValue(resourceType)
						.appendText(" owned by ")
						.appendText(owner);
		}
		
		@Override
		public void describeMismatch(Object item, Description description) {
			description.appendText("the policy ");
			description.appendValue(item);
			description.appendText(" was unable to");
		}
		
	}

}