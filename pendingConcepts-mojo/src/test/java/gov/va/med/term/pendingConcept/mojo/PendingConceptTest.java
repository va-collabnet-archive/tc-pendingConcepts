package gov.va.med.term.pendingConcept.mojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

public class PendingConceptTest  {
	
	@Test
    public void testProperPCSctId() {
		PendingConcept pc = new PendingConcept(123L, "Test pending concept", 456L, "Parent concept");
		assertEquals("Pending Concept's SNOMED CT Identifer matches test failed", pc.getSCTID(), 123L);

    }

	@Test
    public void testProperParentSctId() {
		PendingConcept pc = new PendingConcept(123L, "Test pending concept", 456L, "Parent concept");
        assertEquals("Pending Concept's parent's SNOMED CT Identifer matches test failed", pc.getParentSCTID(), 456L);

    }

	@Test
    public void testProperPCDescription() {
		PendingConcept pc = new PendingConcept(123L, "Test pending concept", 456L, "Parent concept");
        assertEquals("Pending Concept's description matches test failed", pc.getFSN(), "Test pending concept");

    }

	@Test
    public void testImproperParentSctDescription() {
		PendingConcept pc = new PendingConcept(123L, "Test pending concept", 456L, "Parent concept");
		assertEquals("Pending Concept's parent's description matches test failed", pc.getParentDescription(), "Parent concept");

    }
	@Test
    public void testImproperPCSctId() {
		PendingConcept pc = new PendingConcept(123L, "Test pending concept", 456L, "Parent concept");
		assertNotSame ("Pending Concept's SNOMED CT Identifer NOT matches test failed", pc.getSCTID(), 789L);

    }

	@Test
    public void testImproperParentSctId() {
		PendingConcept pc = new PendingConcept(123L, "Test pending concept", 456L, "Parent concept");
		assertNotSame ("Pending Concept's parent's SNOMED CT Identifer NOT matches test failed", pc.getParentSCTID(), 789L);

    }

	@Test
    public void testImproperPCDescription() {
		PendingConcept pc = new PendingConcept(123L, "Test pending concept", 456L, "Parent concept");
		assertNotSame ("Pending Concept's description NOT matches test failed", pc.getFSN(), "Foo");
    }

	@Test
    public void testProperParentSctDescription() {
		PendingConcept pc = new PendingConcept(123L, "Test pending concept", 456L, "Parent concept");
		assertNotSame("Pending Concept's parent's description NOT matches test failed", pc.getParentDescription(), "Foo");
    }
	
}
