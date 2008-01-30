/**
 * $Id$
 * $URL$
 * EvalEvaluationServiceImplTest.java - evaluation - Jan 28, 2008 6:01:13 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Centre for Applied Research in Educational Technologies, University of Cambridge
 * Licensed under the Educational Community License version 1.0
 * 
 * A copy of the Educational Community License has been included in this 
 * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
 *
 * Aaron Zeckoski (azeckoski@gmail.com) (aaronz@vt.edu) (aaron@caret.cam.ac.uk)
 */

package org.sakaiproject.evaluation.logic.test;

import java.util.List;
import java.util.Map;

import org.sakaiproject.evaluation.dao.EvaluationDao;
import org.sakaiproject.evaluation.logic.EvalSettings;
import org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl;
import org.sakaiproject.evaluation.logic.impl.EvalSecurityChecks;
import org.sakaiproject.evaluation.logic.model.EvalGroup;
import org.sakaiproject.evaluation.model.EvalAnswer;
import org.sakaiproject.evaluation.model.EvalAssignGroup;
import org.sakaiproject.evaluation.model.EvalAssignHierarchy;
import org.sakaiproject.evaluation.model.EvalEvaluation;
import org.sakaiproject.evaluation.model.EvalResponse;
import org.sakaiproject.evaluation.model.constant.EvalConstants;
import org.sakaiproject.evaluation.test.EvalTestDataLoad;
import org.sakaiproject.evaluation.test.PreloadTestData;
import org.sakaiproject.evaluation.test.mocks.MockEvalExternalLogic;
import org.springframework.test.AbstractTransactionalSpringContextTests;


/**
 * Tests for the EvalEvaluationServiceImpl
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class EvalEvaluationServiceImplTest extends AbstractTransactionalSpringContextTests {

   protected EvalEvaluationServiceImpl evaluationService;

   private EvaluationDao evaluationDao;
   private EvalTestDataLoad etdl;

   protected String[] getConfigLocations() {
      // point to the needed spring config files, must be on the classpath
      // (add component/src/webapp/WEB-INF to the build path in Eclipse),
      // they also need to be referenced in the project.xml file
      return new String[] {"hibernate-test.xml", "spring-hibernate.xml", "logic-support.xml"};
   }

   // run this before each test starts
   protected void onSetUpBeforeTransaction() throws Exception {

      // load the spring created dao class bean from the Spring Application Context
      evaluationDao = (EvaluationDao) applicationContext.getBean("org.sakaiproject.evaluation.dao.EvaluationDao");
      if (evaluationDao == null) {
         throw new NullPointerException("EvaluationDao could not be retrieved from spring context");
      }

      // check the preloaded test data
      assertTrue("Error preloading test data", evaluationDao.countAll(EvalEvaluation.class) > 0);

      PreloadTestData ptd = (PreloadTestData) applicationContext.getBean("org.sakaiproject.evaluation.test.PreloadTestData");
      if (ptd == null) {
         throw new NullPointerException("PreloadTestData could not be retrieved from spring context");
      }

      // get test objects
      etdl = ptd.getEtdl();

      // load up any other needed spring beans
      EvalSettings settings = (EvalSettings) applicationContext.getBean("org.sakaiproject.evaluation.logic.EvalSettings");
      if (settings == null) {
         throw new NullPointerException("EvalSettings could not be retrieved from spring evalGroupId");
      }

      EvalSecurityChecks securityChecks = (EvalSecurityChecks) applicationContext.getBean("org.sakaiproject.evaluation.logic.impl.EvalSecurityChecks");
      if (settings == null) {
         throw new NullPointerException("EvalSecurityChecks could not be retrieved from spring context");
      }

      // setup the mock objects if needed

      // create and setup the object to be tested
      evaluationService = new EvalEvaluationServiceImpl();
      evaluationService.setDao(evaluationDao);
      evaluationService.setExternalLogic( new MockEvalExternalLogic() );
      evaluationService.setSecurityChecks(securityChecks);
      evaluationService.setSettings(settings);

   }

   // run this before each test starts and as part of the transaction
   protected void onSetUpInTransaction() {
      // preload additional data if desired

   }


   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getEvaluationById(java.lang.Long)}.
    */
   public void testGetEvaluationById() {
      EvalEvaluation eval = evaluationService.getEvaluationById(etdl.evaluationActive.getId());
      assertNotNull(eval);
      assertNotNull(eval.getBlankResponsesAllowed());
      assertNotNull(eval.getModifyResponsesAllowed());
      assertNotNull(eval.getResultsPrivate());
      assertNotNull(eval.getUnregisteredAllowed());
      assertEquals(etdl.evaluationActive.getId(), eval.getId());

      eval = evaluationService.getEvaluationById(etdl.evaluationNew.getId());
      assertNotNull(eval);
      assertEquals(etdl.evaluationNew.getId(), eval.getId());

      // test get eval by invalid id
      eval = evaluationService.getEvaluationById( EvalTestDataLoad.INVALID_LONG_ID );
      assertNull(eval);
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#checkEvaluationExists(java.lang.Long)}.
    */
   public void testCheckEvaluationExists() {
      // positive
      assertTrue(evaluationService.checkEvaluationExists(etdl.evaluationActive.getId()));
      assertTrue(evaluationService.checkEvaluationExists(etdl.evaluationClosed.getId()));

      // negative
      assertFalse(evaluationService.checkEvaluationExists(EvalTestDataLoad.INVALID_LONG_ID));

      // exception
      try {
         evaluationService.checkEvaluationExists(null);
         fail("Should have thrown exception");
      } catch (NullPointerException e) {
         assertNotNull(e);
      }
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#countEvaluationsByTemplateId(java.lang.Long)}.
    */
   public void testCountEvaluationsByTemplateId() {
      // test valid template ids
      int count = evaluationService.countEvaluationsByTemplateId( etdl.templatePublic.getId() );
      assertEquals(2, count);

      count = evaluationService.countEvaluationsByTemplateId( etdl.templateUser.getId() );
      assertEquals(3, count);

      // test no evaluations for a template
      count = evaluationService.countEvaluationsByTemplateId( etdl.templateUnused.getId() );
      assertEquals(0, count);

      // test invalid template id
      try {
         count = evaluationService.countEvaluationsByTemplateId( EvalTestDataLoad.INVALID_LONG_ID );
         fail("Should have thrown exception");
      } catch (RuntimeException e) {
         assertNotNull(e);
      }
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getEvaluationByEid(java.lang.String)}.
    */
   public void testGetEvaluationByEid() {
      EvalEvaluation evaluation = null;

      // test getting evaluation having eid set
      evaluation = evaluationService.getEvaluationByEid( etdl.evaluationProvided.getEid() );
      assertNotNull(evaluation);
      assertEquals(etdl.evaluationProvided.getEid(), evaluation.getEid());

      //test getting evaluation having eid not set  returns null
      evaluation = evaluationService.getEvaluationByEid( etdl.evaluationActive.getEid() );
      assertNull(evaluation);

      // test getting evaluation by invalid eid returns null
      evaluation = evaluationService.getEvaluationByEid( EvalTestDataLoad.INVALID_STRING_EID );
      assertNull(evaluation);
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getEvaluationsByTemplateId(java.lang.Long)}.
    */
   public void testGetEvaluationsByTemplateId() {
      List<EvalEvaluation> l = null;
      List<Long> ids = null;

      // test valid template ids
      l = evaluationService.getEvaluationsByTemplateId( etdl.templatePublic.getId() );
      assertNotNull(l);
      assertEquals(2, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.evaluationNew.getId() ));
      assertTrue(ids.contains( etdl.evaluationActiveUntaken.getId() ));

      l = evaluationService.getEvaluationsByTemplateId( etdl.templateUser.getId() );
      assertNotNull(l);
      assertEquals(3, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.evaluationActive.getId() ));
      assertTrue(ids.contains( etdl.evaluationViewable.getId() ));
      assertTrue(ids.contains( etdl.evaluationProvided.getId() ));

      // test no evaluations for a template
      l = evaluationService.getEvaluationsByTemplateId( etdl.templateUnused.getId() );
      assertNotNull(l);
      assertTrue(l.isEmpty());

      // test invalid template id
      try {
         l = evaluationService.getEvaluationsByTemplateId( EvalTestDataLoad.INVALID_LONG_ID );
         fail("Should have thrown exception");
      } catch (RuntimeException e) {
         assertNotNull(e);
      }
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#updateEvaluationState(java.lang.Long)}.
    */
   public void testUpdateEvaluationStateLong() {
      assertEquals( evaluationService.updateEvaluationState( etdl.evaluationNew.getId() ), EvalConstants.EVALUATION_STATE_INQUEUE );
      assertEquals( evaluationService.updateEvaluationState( etdl.evaluationActive.getId() ), EvalConstants.EVALUATION_STATE_ACTIVE );
      assertEquals( evaluationService.updateEvaluationState( etdl.evaluationClosed.getId() ), EvalConstants.EVALUATION_STATE_CLOSED );
      assertEquals( evaluationService.updateEvaluationState( etdl.evaluationViewable.getId() ), EvalConstants.EVALUATION_STATE_VIEWABLE );

      try {
         evaluationService.updateEvaluationState( EvalTestDataLoad.INVALID_LONG_ID );
         fail("Should have thrown exception");
      } catch (RuntimeException e) {
         assertNotNull(e);
      }

      // TODO - add tests for changing state when checked
   }

   public void testCanBeginEvaluation() {
      assertTrue( evaluationService.canBeginEvaluation(EvalTestDataLoad.ADMIN_USER_ID) );
      assertTrue( evaluationService.canBeginEvaluation(EvalTestDataLoad.MAINT_USER_ID) );
      assertFalse( evaluationService.canBeginEvaluation(EvalTestDataLoad.USER_ID) );
      assertFalse( evaluationService.canBeginEvaluation(EvalTestDataLoad.INVALID_USER_ID) );
   }

   public void testCanTakeEvaluation() {
      // test able to take untaken eval
      assertTrue( evaluationService.canTakeEvaluation( EvalTestDataLoad.USER_ID, 
            etdl.evaluationActiveUntaken.getId(), EvalTestDataLoad.SITE1_REF) );
      // test able to take eval in evalGroupId not taken in yet
      assertTrue( evaluationService.canTakeEvaluation( EvalTestDataLoad.USER_ID, 
            etdl.evaluationActiveUntaken.getId(), EvalTestDataLoad.SITE1_REF) );
      // test admin can always take
      assertTrue( evaluationService.canTakeEvaluation( EvalTestDataLoad.ADMIN_USER_ID, 
            etdl.evaluationActive.getId(), EvalTestDataLoad.SITE1_REF) );
      // anonymous can always be taken
      assertTrue( evaluationService.canTakeEvaluation( EvalTestDataLoad.MAINT_USER_ID, 
            etdl.evaluationActiveUntaken.getId(), EvalTestDataLoad.SITE1_REF) );

      // test not able to take
      // not assigned to this group
      assertFalse( evaluationService.canTakeEvaluation( EvalTestDataLoad.USER_ID, 
            etdl.evaluationActiveUntaken.getId(), EvalTestDataLoad.SITE2_REF) );
      // already taken
      assertFalse( evaluationService.canTakeEvaluation( EvalTestDataLoad.USER_ID, 
            etdl.evaluationActive.getId(), EvalTestDataLoad.SITE1_REF) );
      // not assigned to this evalGroupId
      assertFalse( evaluationService.canTakeEvaluation( EvalTestDataLoad.USER_ID, 
            etdl.evaluationActive.getId(), EvalTestDataLoad.SITE2_REF) );
      // cannot take evaluation (no perm)
      assertFalse( evaluationService.canTakeEvaluation( EvalTestDataLoad.MAINT_USER_ID, 
            etdl.evaluationActive.getId(), EvalTestDataLoad.SITE1_REF) );

      // test invalid information
      assertFalse( evaluationService.canTakeEvaluation( EvalTestDataLoad.USER_ID, 
            etdl.evaluationActiveUntaken.getId(), EvalTestDataLoad.INVALID_CONTEXT) );
      assertFalse( evaluationService.canTakeEvaluation( EvalTestDataLoad.INVALID_USER_ID, 
            etdl.evaluationActive.getId(), EvalTestDataLoad.SITE1_REF) );

      try {
         evaluationService.canTakeEvaluation( EvalTestDataLoad.USER_ID, 
               EvalTestDataLoad.INVALID_LONG_ID, EvalTestDataLoad.SITE1_REF);
         fail("Should have thrown exception");
      } catch (RuntimeException e) {
         assertNotNull(e);
      }

   }

   public void testCanControlEvaluation() {
      // test can control
      assertTrue( evaluationService.canControlEvaluation(
            EvalTestDataLoad.MAINT_USER_ID, etdl.evaluationNew.getId() ) );

      // test can control (admin user id)
      assertTrue( evaluationService.canControlEvaluation(
            EvalTestDataLoad.ADMIN_USER_ID, etdl.evaluationNew.getId() ) );

      // test cannot control (non owner)
      assertFalse( evaluationService.canControlEvaluation(
            EvalTestDataLoad.USER_ID, etdl.evaluationNew.getId() ) );

      // test can control (active)
      assertTrue( evaluationService.canControlEvaluation(
            EvalTestDataLoad.ADMIN_USER_ID, etdl.evaluationActive.getId() ) );

      // test can control (closed and viewable)
      assertTrue( evaluationService.canControlEvaluation(
            EvalTestDataLoad.ADMIN_USER_ID, etdl.evaluationViewable.getId() ) );
   }

   public void testCanRemoveEvaluation() {
      // test can remove
      assertTrue( evaluationService.canRemoveEvaluation(
            EvalTestDataLoad.MAINT_USER_ID, etdl.evaluationNew.getId() ) );

      // test can remove (admin user id)
      assertTrue( evaluationService.canRemoveEvaluation(
            EvalTestDataLoad.ADMIN_USER_ID, etdl.evaluationNew.getId() ) );

      // test cannot remove (non owner)
      assertFalse( evaluationService.canRemoveEvaluation(
            EvalTestDataLoad.USER_ID, etdl.evaluationNew.getId() ) );

      // test cannot remove (active)
      assertFalse( evaluationService.canRemoveEvaluation(
            EvalTestDataLoad.ADMIN_USER_ID, etdl.evaluationActive.getId() ) );

      // test cannot remove (closed and viewable)
      assertFalse( evaluationService.canRemoveEvaluation(
            EvalTestDataLoad.ADMIN_USER_ID, etdl.evaluationViewable.getId() ) );
   }


   // EVAL AND GROUP ASSIGNS

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#countEvaluationGroups(java.lang.Long)}.
    */
   public void testCountEvaluationGroups() {
      int count = evaluationService.countEvaluationGroups( etdl.evaluationClosed.getId() );
      assertEquals(2, count);

      count = evaluationService.countEvaluationGroups( etdl.evaluationActive.getId() );
      assertEquals(1, count);

      // test no assigned contexts
      count = evaluationService.countEvaluationGroups( etdl.evaluationNew.getId() );
      assertEquals(0, count);

      // test invalid
      count = evaluationService.countEvaluationGroups( EvalTestDataLoad.INVALID_LONG_ID );
      assertEquals(0, count);
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getAssignGroupById(java.lang.Long)}.
    */
   public void testGetAssignGroupById() {
      EvalAssignGroup assignGroup = null;

      // test getting valid items by id
      assignGroup = evaluationService.getAssignGroupById( etdl.assign1.getId() );
      assertNotNull(assignGroup);
      assertEquals(etdl.assign1.getId(), assignGroup.getId());

      assignGroup = evaluationService.getAssignGroupById( etdl.assign2.getId() );
      assertNotNull(assignGroup);
      assertEquals(etdl.assign2.getId(), assignGroup.getId());

      // test get eval by invalid id returns null
      assignGroup = evaluationService.getAssignGroupById( EvalTestDataLoad.INVALID_LONG_ID );
      assertNull(assignGroup);
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getAssignGroupByEid(java.lang.String)}.
    */
   public void testGetAssignGroupByEid() {
      EvalAssignGroup assignGroupProvided = null;

      // test getting assignGroup having eid set
      assignGroupProvided = evaluationService.getAssignGroupByEid( etdl.assignGroupProvided.getEid() );
      assertNotNull(assignGroupProvided);
      assertEquals(etdl.assignGroupProvided.getEid(), assignGroupProvided.getEid());

      //test getting assignGroup having eid not set  returns null
      assignGroupProvided = evaluationService.getAssignGroupByEid( etdl.assign7.getEid() );
      assertNull(assignGroupProvided);

      // test getting assignGroup by invalid eid returns null
      assignGroupProvided = evaluationService.getAssignGroupByEid( EvalTestDataLoad.INVALID_STRING_EID );
      assertNull(assignGroupProvided);
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getAssignGroupId(java.lang.Long, java.lang.String)}.
    */
   public void testGetAssignGroupId() {
      Long assignGroupId = null;

      // test getting valid items by id
      assignGroupId = evaluationService.getAssignGroupId( etdl.evaluationActive.getId(), EvalTestDataLoad.SITE1_REF );
      assertNotNull(assignGroupId);
      assertEquals(etdl.assign1.getId(), assignGroupId);

      assignGroupId = evaluationService.getAssignGroupId( etdl.evaluationClosed.getId(), EvalTestDataLoad.SITE2_REF );
      assertNotNull(assignGroupId);
      assertEquals(etdl.assign4.getId(), assignGroupId);

      // test invalid evaluation/group mixture returns null
      assignGroupId = evaluationService.getAssignGroupId( etdl.evaluationActive.getId(), EvalTestDataLoad.SITE2_REF );
      assertNull("Found an id?: " + assignGroupId, assignGroupId);

      assignGroupId = evaluationService.getAssignGroupId( etdl.evaluationViewable.getId(), EvalTestDataLoad.SITE1_REF );
      assertNull(assignGroupId);

      // test get by invalid id returns null
      assignGroupId = evaluationService.getAssignGroupId( EvalTestDataLoad.INVALID_LONG_ID, EvalTestDataLoad.INVALID_CONSTANT_STRING );
      assertNull(assignGroupId);
   }


   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getAssignHierarchyById(java.lang.Long)}.
    */
   public void testGetAssignHierarchyById() {
      EvalAssignHierarchy eah = null;

      eah = evaluationService.getAssignHierarchyById(etdl.assignHier1.getId());
      assertNotNull(eah);
      assertEquals(etdl.assignHier1.getId(), eah.getId());

      eah = evaluationService.getAssignHierarchyById(EvalTestDataLoad.INVALID_LONG_ID);
      assertNull(eah);

      try {
         evaluationService.getAssignHierarchyById(null);
         fail("Should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getAssignHierarchyByEval(java.lang.Long)}.
    */
   public void testGetAssignHierarchyByEval() {
      List<EvalAssignHierarchy> eahs = null;

      eahs = evaluationService.getAssignHierarchyByEval(etdl.evaluationActive.getId());
      assertNotNull(eahs);
      assertEquals(1, eahs.size());
      assertEquals(etdl.assignHier1.getId(), eahs.get(0).getId());

      eahs = evaluationService.getAssignHierarchyByEval(etdl.evaluationNew.getId());
      assertNotNull(eahs);
      assertEquals(0, eahs.size());

      try {
         evaluationService.getAssignHierarchyByEval(null);
         fail("Should have thrown exception");
      } catch (NullPointerException e) {
         assertNotNull(e);
      }
   }


   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getEvaluationGroups(java.lang.Long[], boolean)}.
    */
   public void testGetEvaluationGroups() {
      Map<Long, List<EvalGroup>> m = evaluationService.getEvaluationGroups( 
            new Long[] { etdl.evaluationClosed.getId() }, true );
      assertNotNull(m);
      List<EvalGroup> evalGroups = m.get( etdl.evaluationClosed.getId() );
      assertNotNull(evalGroups);
      assertEquals(2, evalGroups.size());
      assertTrue( evalGroups.get(0) instanceof EvalGroup );
      assertTrue( evalGroups.get(1) instanceof EvalGroup );

      m = evaluationService.getEvaluationGroups( 
            new Long[] { etdl.evaluationActive.getId() }, true );
      assertNotNull(m);
      evalGroups = m.get( etdl.evaluationActive.getId() );
      assertNotNull(evalGroups);
      assertEquals(1, evalGroups.size());
      assertTrue( evalGroups.get(0) instanceof EvalGroup );
      assertEquals( EvalTestDataLoad.SITE1_REF, ((EvalGroup) evalGroups.get(0)).evalGroupId );

      // test no assigned contexts
      m = evaluationService.getEvaluationGroups( 
            new Long[] { etdl.evaluationNew.getId() }, true );
      assertNotNull(m);
      evalGroups = m.get( etdl.evaluationNew.getId() );
      assertNotNull(evalGroups);
      assertEquals(0, evalGroups.size());

      // test invalid
      m = evaluationService.getEvaluationGroups( 
            new Long[] { EvalTestDataLoad.INVALID_LONG_ID }, true );
      assertNotNull(m);
      evalGroups = m.get( EvalTestDataLoad.INVALID_LONG_ID );
      assertNotNull(evalGroups);
      assertEquals(0, evalGroups.size());
   }


   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getEvaluationAssignGroups(java.lang.Long[], boolean)}.
    */
   public void testGetEvaluationAssignGroups() {
      // this is mostly tested above
      Map<Long, List<EvalAssignGroup>> m = evaluationService.getEvaluationAssignGroups( 
            new Long[] { etdl.evaluationClosed.getId() }, true );
      assertNotNull(m);
      List<EvalAssignGroup> eags = m.get( etdl.evaluationClosed.getId() );
      assertNotNull(eags);
      assertEquals(2, eags.size());
      assertTrue( eags.get(0) instanceof EvalAssignGroup );
      assertTrue( eags.get(1) instanceof EvalAssignGroup );
      assertEquals(etdl.assign3.getId(), eags.get(0).getId());
      assertEquals(etdl.assign4.getId(), eags.get(1).getId());
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#canCreateAssignEval(java.lang.String, java.lang.Long)}.
    */
   public void testCanCreateAssignEval() {
      // test can create an AC in new
      assertTrue( evaluationService.canCreateAssignEval(
            EvalTestDataLoad.MAINT_USER_ID,  etdl.evaluationNew.getId()) );
      assertTrue( evaluationService.canCreateAssignEval(
            EvalTestDataLoad.ADMIN_USER_ID,  etdl.evaluationNew.getId()) );
      assertTrue( evaluationService.canCreateAssignEval(
            EvalTestDataLoad.ADMIN_USER_ID,  etdl.evaluationNewAdmin.getId()) );
      assertTrue( evaluationService.canCreateAssignEval(
            EvalTestDataLoad.MAINT_USER_ID,  etdl.evaluationActive.getId()) );
      assertTrue( evaluationService.canCreateAssignEval(
            EvalTestDataLoad.ADMIN_USER_ID,  etdl.evaluationActive.getId()) );

      // test cannot create AC in closed evals
      assertFalse( evaluationService.canCreateAssignEval(
            EvalTestDataLoad.MAINT_USER_ID,  etdl.evaluationClosed.getId()) );
      assertFalse( evaluationService.canCreateAssignEval(
            EvalTestDataLoad.MAINT_USER_ID,  etdl.evaluationViewable.getId()) );

      // test cannot create AC without perms
      assertFalse( evaluationService.canCreateAssignEval(
            EvalTestDataLoad.USER_ID, etdl.evaluationNew.getId()) );
      assertFalse( evaluationService.canCreateAssignEval(
            EvalTestDataLoad.MAINT_USER_ID,  etdl.evaluationNewAdmin.getId()) );

      // test invalid evaluation id
      try {
         evaluationService.canCreateAssignEval(
               EvalTestDataLoad.MAINT_USER_ID,  EvalTestDataLoad.INVALID_LONG_ID );
         fail("Should have thrown exception");
      } catch (RuntimeException e) {
         assertNotNull(e);
      }
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#canDeleteAssignGroup(java.lang.String, java.lang.Long)}.
    */
   public void testCanDeleteAssignGroup() {
      // test can remove an AC in new eval
      assertTrue( evaluationService.canDeleteAssignGroup(
            EvalTestDataLoad.MAINT_USER_ID,  etdl.assign6.getId()) );
      assertTrue( evaluationService.canDeleteAssignGroup(
            EvalTestDataLoad.ADMIN_USER_ID,  etdl.assign6.getId()) );
      assertTrue( evaluationService.canDeleteAssignGroup(
            EvalTestDataLoad.ADMIN_USER_ID,  etdl.assign7.getId()) );

      // test cannot remove AC from running evals
      assertFalse( evaluationService.canDeleteAssignGroup(
            EvalTestDataLoad.MAINT_USER_ID, etdl.assign1.getId()) );
      assertFalse( evaluationService.canDeleteAssignGroup(
            EvalTestDataLoad.MAINT_USER_ID, etdl.assign4.getId()) );
      assertFalse( evaluationService.canDeleteAssignGroup(
            EvalTestDataLoad.ADMIN_USER_ID, etdl.assign3.getId()) );
      assertFalse( evaluationService.canDeleteAssignGroup(
            EvalTestDataLoad.ADMIN_USER_ID, etdl.assign5.getId()) );

      // test cannot remove without permission
      assertFalse( evaluationService.canDeleteAssignGroup(
            EvalTestDataLoad.USER_ID, etdl.assign6.getId()) );
      assertFalse( evaluationService.canDeleteAssignGroup(
            EvalTestDataLoad.MAINT_USER_ID, etdl.assign7.getId()) );

      // test invalid evaluation id
      try {
         evaluationService.canDeleteAssignGroup(
               EvalTestDataLoad.MAINT_USER_ID,  EvalTestDataLoad.INVALID_LONG_ID );
         fail("Should have thrown exception");
      } catch (RuntimeException e) {
         assertNotNull(e);
      }
   }



   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getResponseById(java.lang.Long)}.
    */
   public void testGetResponseById() {
      EvalResponse response = null;

      response = evaluationService.getResponseById( etdl.response1.getId() );
      assertNotNull(response);
      assertEquals(etdl.response1.getId(), response.getId());

      response = evaluationService.getResponseById( etdl.response2.getId() );
      assertNotNull(response);
      assertEquals(etdl.response2.getId(), response.getId());

      // test get eval by invalid id
      response = evaluationService.getResponseById( EvalTestDataLoad.INVALID_LONG_ID );
      assertNull(response);
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getEvaluationResponseForUserAndGroup(java.lang.Long, java.lang.String, java.lang.String)}.
    */
   public void testGetEvaluationResponseForUserAndGroup() {
      EvalResponse response = null;

      // check retrieving an existing responses
      response = evaluationService.getEvaluationResponseForUserAndGroup(etdl.evaluationClosed.getId(), EvalTestDataLoad.USER_ID, EvalTestDataLoad.SITE1_REF);
      assertNotNull(response);
      assertEquals(etdl.response2.getId(), response.getId());

      // check creating a new response
      response = evaluationService.getEvaluationResponseForUserAndGroup(etdl.evaluationActiveUntaken.getId(), EvalTestDataLoad.USER_ID, EvalTestDataLoad.SITE1_REF);
      assertNull(response);

      // test invalid params fails
      try {
         evaluationService.getEvaluationResponseForUserAndGroup(EvalTestDataLoad.INVALID_LONG_ID, EvalTestDataLoad.USER_ID, EvalTestDataLoad.SITE1_REF);
         fail("Should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }
   }

   public void testGetEvalResponseIds() {
      List<Long> l = null;

      // retrieve all response Ids for an evaluation
      l = evaluationService.getEvalResponseIds(etdl.evaluationClosed.getId(), null, true);
      assertNotNull(l);
      assertEquals(3, l.size());
      assertTrue(l.contains( etdl.response2.getId() ));
      assertTrue(l.contains( etdl.response3.getId() ));
      assertTrue(l.contains( etdl.response6.getId() ));

      l = evaluationService.getEvalResponseIds(etdl.evaluationClosed.getId(), new String[] {}, true);
      assertNotNull(l);
      assertEquals(3, l.size());
      assertTrue(l.contains( etdl.response2.getId() ));
      assertTrue(l.contains( etdl.response3.getId() ));
      assertTrue(l.contains( etdl.response6.getId() ));

      // retrieve all response Ids for an evaluation using all groups
      l = evaluationService.getEvalResponseIds(etdl.evaluationClosed.getId(), 
            new String[] {EvalTestDataLoad.SITE1_REF, EvalTestDataLoad.SITE2_REF}, true);
      assertNotNull(l);
      assertEquals(3, l.size());
      assertTrue(l.contains( etdl.response2.getId() ));
      assertTrue(l.contains( etdl.response3.getId() ));
      assertTrue(l.contains( etdl.response6.getId() ));

      // test retrieval of all responses for an evaluation
      l = evaluationService.getEvalResponseIds(etdl.evaluationClosed.getId(), 
            new String[] {EvalTestDataLoad.SITE1_REF, EvalTestDataLoad.SITE2_REF}, null);
      assertNotNull(l);
      assertEquals(3, l.size());

      // test retrieval of incomplete responses for an evaluation
      l = evaluationService.getEvalResponseIds(etdl.evaluationClosed.getId(), 
            new String[] {EvalTestDataLoad.SITE1_REF, EvalTestDataLoad.SITE2_REF}, false);
      assertNotNull(l);
      assertEquals(0, l.size());

      // retrieve all response Ids for an evaluation in one group only
      l = evaluationService.getEvalResponseIds(etdl.evaluationClosed.getId(), new String[] {EvalTestDataLoad.SITE1_REF}, true);
      assertNotNull(l);
      assertEquals(1, l.size());
      assertTrue(l.contains( etdl.response2.getId() ));

      // retrieve all response Ids for an evaluation in one group only
      l = evaluationService.getEvalResponseIds(etdl.evaluationClosed.getId(), new String[] {EvalTestDataLoad.SITE2_REF}, true);
      assertNotNull(l);
      assertEquals(2, l.size());
      assertTrue(l.contains( etdl.response3.getId() ));
      assertTrue(l.contains( etdl.response6.getId() ));

      l = evaluationService.getEvalResponseIds(etdl.evaluationActive.getId(), new String[] {EvalTestDataLoad.SITE1_REF}, true);
      assertNotNull(l);
      assertEquals(1, l.size());
      assertTrue(l.contains( etdl.response1.getId() ));

      // try to get responses for an eval group that is not associated with this eval
      l = evaluationService.getEvalResponseIds(etdl.evaluationActive.getId(), new String[] {EvalTestDataLoad.SITE2_REF}, true);
      assertNotNull(l);
      assertEquals(0, l.size());

      // try to get responses for an eval with no responses
      l = evaluationService.getEvalResponseIds(etdl.evaluationActiveUntaken.getId(), null, true);
      assertNotNull(l);
      assertEquals(0, l.size());

      // check that invalid eval ids cause failure
      try {
         l = evaluationService.getEvalResponseIds( EvalTestDataLoad.INVALID_LONG_ID, null, true);
         fail("Should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }

   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getEvaluationResponses(java.lang.String, java.lang.Long[], java.lang.String, java.lang.Boolean)}.
    */
   public void testGetEvaluationResponses() {
      List<EvalResponse> l = null;
      List<Long> ids = null;

      // retrieve response objects for all fields known
      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.USER_ID, 
            new Long[] { etdl.evaluationClosed.getId() }, new String[] {EvalTestDataLoad.SITE1_REF}, true);
      assertNotNull(l);
      assertEquals(1, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.response2.getId() ));

      // retrieve all responses for a user in an evaluation
      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.USER_ID, 
            new Long[] { etdl.evaluationClosed.getId() }, null, true);
      assertNotNull(l);
      assertEquals(2, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.response2.getId() ));
      assertTrue(ids.contains( etdl.response6.getId() ));

      // retrieve one response for a normal user in one eval
      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.USER_ID, 
            new Long[] { etdl.evaluationActive.getId() }, null, true );
      assertNotNull(l);
      assertEquals(1, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.response1.getId() ));

      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.STUDENT_USER_ID, 
            new Long[] { etdl.evaluationClosed.getId() }, null, true );
      assertNotNull(l);
      assertEquals(1, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.response3.getId() ));

      // check that empty array is ok for eval group ids
      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.USER_ID, 
            new Long[] { etdl.evaluationActive.getId() }, new String[] {}, true );
      assertNotNull(l);
      assertEquals(1, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.response1.getId() ));

      // retrieve all responses for a normal user in one eval
      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.USER_ID, 
            new Long[] { etdl.evaluationClosed.getId() }, null, true );
      assertNotNull(l);
      assertEquals(2, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.response2.getId() ));
      assertTrue(ids.contains( etdl.response6.getId() ));

      // limit retrieval by eval groups ids
      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.USER_ID, 
            new Long[] { etdl.evaluationClosed.getId() }, new String[] {EvalTestDataLoad.SITE1_REF}, true );
      assertNotNull(l);
      assertEquals(1, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.response2.getId() ));

      // retrieve all responses for a normal user in mutliple evals
      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.USER_ID, 
            new Long[] { etdl.evaluationActive.getId(), etdl.evaluationClosed.getId(), etdl.evaluationViewable.getId() }, null, true );
      assertNotNull(l);
      assertEquals(4, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.response1.getId() ));
      assertTrue(ids.contains( etdl.response2.getId() ));
      assertTrue(ids.contains( etdl.response4.getId() ));
      assertTrue(ids.contains( etdl.response6.getId() ));

      // attempt to retrieve all responses
      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.USER_ID, 
            new Long[] { etdl.evaluationActive.getId(), etdl.evaluationClosed.getId(), etdl.evaluationViewable.getId() }, null, null );
      assertNotNull(l);
      assertEquals(4, l.size());

      // attempt to retrieve all incomplete responses (there are none)
      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.USER_ID, 
            new Long[] { etdl.evaluationActive.getId(), etdl.evaluationClosed.getId(), etdl.evaluationViewable.getId() }, null, false );
      assertNotNull(l);
      assertEquals(0, l.size());

      // attempt to retrieve results for user that has no responses
      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.STUDENT_USER_ID, 
            new Long[] { etdl.evaluationActive.getId() }, null, true );
      assertNotNull(l);
      assertEquals(0, l.size());

      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.MAINT_USER_ID, 
            new Long[] { etdl.evaluationActive.getId(), etdl.evaluationClosed.getId(), etdl.evaluationViewable.getId() }, null, true );
      assertNotNull(l);
      assertEquals(0, l.size());

      // test that admin can fetch all results for evaluations
      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.ADMIN_USER_ID, 
            new Long[] { etdl.evaluationActive.getId(), etdl.evaluationClosed.getId(), etdl.evaluationViewable.getId() }, null, true );
      assertNotNull(l);
      assertEquals(6, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.response1.getId() ));
      assertTrue(ids.contains( etdl.response2.getId() ));
      assertTrue(ids.contains( etdl.response3.getId() ));
      assertTrue(ids.contains( etdl.response4.getId() ));
      assertTrue(ids.contains( etdl.response5.getId() ));
      assertTrue(ids.contains( etdl.response6.getId() ));

      l = evaluationService.getEvaluationResponses(EvalTestDataLoad.ADMIN_USER_ID, 
            new Long[] { etdl.evaluationViewable.getId() }, null, true );
      assertNotNull(l);
      assertEquals(2, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.response4.getId() ));
      assertTrue(ids.contains( etdl.response5.getId() ));

      // check that empty array causes failure
      try {
         l = evaluationService.getEvaluationResponses(EvalTestDataLoad.ADMIN_USER_ID, 
               new Long[] {}, null, true );
         fail("Should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }

      // check that null evalids cause failure
      try {
         l = evaluationService.getEvaluationResponses(EvalTestDataLoad.ADMIN_USER_ID, 
               null, null, true );
         fail("Should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#countEvaluationResponses(java.lang.String, java.lang.Long[], java.lang.String, java.lang.Boolean)}.
    */
   public void testCountEvaluationResponses() {
      // test counts for all responses in various evaluations
      assertEquals(3, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationClosed.getId() }, null, null) );
      assertEquals(2, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationViewable.getId() }, null, null) );
      assertEquals(1, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationActive.getId() }, null, null) );
      assertEquals(0, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationActiveUntaken.getId() }, null, null) );

      // limit by user
      assertEquals(3, evaluationService.countEvaluationResponses(EvalTestDataLoad.ADMIN_USER_ID, new Long[] { etdl.evaluationClosed.getId() }, null, null) );
      assertEquals(2, evaluationService.countEvaluationResponses(EvalTestDataLoad.USER_ID, new Long[] { etdl.evaluationClosed.getId() }, null, null) );
      assertEquals(1, evaluationService.countEvaluationResponses(EvalTestDataLoad.STUDENT_USER_ID, new Long[] { etdl.evaluationClosed.getId() }, null, null) );
      assertEquals(0, evaluationService.countEvaluationResponses(EvalTestDataLoad.MAINT_USER_ID, new Long[] { etdl.evaluationClosed.getId() }, null, null) );

      // test counts limited by evalGroupId
      assertEquals(1, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationClosed.getId() }, 
            new String[] { EvalTestDataLoad.SITE1_REF }, null) );
      assertEquals(0, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationViewable.getId() }, 
            new String[] { EvalTestDataLoad.SITE1_REF }, null) );
      assertEquals(1, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationActive.getId() }, 
            new String[] { EvalTestDataLoad.SITE1_REF }, null) );
      assertEquals(0, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationActiveUntaken.getId() }, 
            new String[] { EvalTestDataLoad.SITE1_REF }, null) );

      assertEquals(2, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationClosed.getId() }, 
            new String[] { EvalTestDataLoad.SITE2_REF }, null) );
      assertEquals(2, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationViewable.getId() }, 
            new String[] { EvalTestDataLoad.SITE2_REF }, null) );
      assertEquals(0, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationActive.getId() }, 
            new String[] { EvalTestDataLoad.SITE2_REF }, null) );
      assertEquals(0, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationActiveUntaken.getId() }, 
            new String[] { EvalTestDataLoad.SITE2_REF }, null) );

      // test counts limited by completed
      assertEquals(3, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationClosed.getId() }, null, true) );
      assertEquals(2, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationViewable.getId() }, null, true) );
      assertEquals(1, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationActive.getId() }, null, true) );
      assertEquals(0, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationActiveUntaken.getId() }, null, true) );

      // test counts limited by incomplete
      assertEquals(0, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationClosed.getId() }, null, false) );
      assertEquals(0, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationViewable.getId() }, null, false) );
      assertEquals(0, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationActive.getId() }, null, false) );
      assertEquals(0, evaluationService.countEvaluationResponses(null, new Long[] { etdl.evaluationActiveUntaken.getId() }, null, false) );

      // check that empty array causes failure
      try {
         evaluationService.countEvaluationResponses(null, new Long[] {}, null, true);
         fail("Should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }

      // check that null evalids cause failure
      try {
         evaluationService.countEvaluationResponses(null, null, null, true);
         fail("Should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#getEvalAnswers(java.lang.Long, java.lang.Long, java.lang.String[])}.
    */
   public void testGetEvalAnswers() {
      List<EvalAnswer> l = null;
      List<Long> ids = null;

      // retrieve one answer for an eval
      l = evaluationService.getEvalAnswers( etdl.item1.getId(), etdl.evaluationActive.getId(), null );
      assertNotNull(l);
      assertEquals(1, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.answer1_1.getId() ));

      l = evaluationService.getEvalAnswers( etdl.item5.getId(), etdl.evaluationClosed.getId(), null );
      assertNotNull(l);
      assertEquals(1, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.answer2_5.getId() ));

      // retrieve multiple answers for an eval
      l = evaluationService.getEvalAnswers( etdl.item1.getId(), etdl.evaluationViewable.getId(), null );
      assertNotNull(l);
      assertEquals(2, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.answer4_1.getId() ));
      assertTrue(ids.contains( etdl.answer5_1.getId() ));

      l = evaluationService.getEvalAnswers( etdl.item2.getId(), etdl.evaluationClosed.getId(), null );
      assertNotNull(l);
      assertEquals(2, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.answer2_2.getId() ));
      assertTrue(ids.contains( etdl.answer3_2.getId() ));

      // retrieve no answers for an eval item
      l = evaluationService.getEvalAnswers( etdl.item1.getId(), etdl.evaluationActiveUntaken.getId(), null );
      assertNotNull(l);
      assertEquals(0, l.size());

      // TODO - add checks which only retrieve partial results for an eval (limit eval groups)

      // TODO - check that invalid item/eval combinations cause failure?

      // check that invalid ids cause failure
      try {
         l = evaluationService.getEvalAnswers( EvalTestDataLoad.INVALID_LONG_ID, etdl.evaluationActiveUntaken.getId(), null );
         fail("Should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }

      try {
         l = evaluationService.getEvalAnswers( etdl.item1.getId(), EvalTestDataLoad.INVALID_LONG_ID, null );
         fail("Should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.logic.impl.EvalEvaluationServiceImpl#canModifyResponse(java.lang.String, java.lang.Long)}.
    */
   public void testCanModifyResponse() {
      // test owner can modify unlocked
      assertTrue( evaluationService.canModifyResponse(
            EvalTestDataLoad.USER_ID, etdl.response1.getId()) );

      // test admin cannot override permissions
      assertFalse( evaluationService.canModifyResponse(
            EvalTestDataLoad.ADMIN_USER_ID,  etdl.response1.getId()) );

      // test users without perms cannot modify
      assertFalse( evaluationService.canModifyResponse(
            EvalTestDataLoad.MAINT_USER_ID,  etdl.response1.getId()) );
      assertFalse( evaluationService.canModifyResponse(
            EvalTestDataLoad.STUDENT_USER_ID, etdl.response1.getId()) );

      // test no one can modify locked responses
      assertFalse( evaluationService.canModifyResponse(
            EvalTestDataLoad.ADMIN_USER_ID,  etdl.response3.getId()) );
      assertFalse( evaluationService.canModifyResponse(
            EvalTestDataLoad.STUDENT_USER_ID, etdl.response3.getId()) );

      // test invalid id causes failure
      try {
         evaluationService.canModifyResponse( EvalTestDataLoad.ADMIN_USER_ID, 
               EvalTestDataLoad.INVALID_LONG_ID );
         fail("Should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }
   }

}