package hudson.plugins.promoted_builds.conditions;

import hudson.ExtensionList;
import hudson.model.FreeStyleBuild;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterDefinition;
import hudson.model.StringParameterDefinition;
import hudson.plugins.promoted_builds.Status;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.conditions.ManualCondition.ManualApproval;

import java.io.IOException;
import java.util.List;

import jenkins.model.Jenkins;

import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

@Bug(22005)
public class ManualConditionBug22005 extends HudsonTestCase {
	private PromotionProcess createPromotionProcess(JobPropertyImpl parent, String name) throws IOException{
        PromotionProcess prom0 = parent.addProcess(name);
        ManualCondition prom0ManualCondition=new ManualCondition();
        prom0ManualCondition.getParameterDefinitions().add(new StringParameterDefinition("param1", prom0.getName()+"_value_1", "Parameter 1"));
        prom0ManualCondition.getParameterDefinitions().add(new StringParameterDefinition("param2", prom0.getName()+"_value_2", "Parameter 2"));
        prom0.conditions.add(prom0ManualCondition);
        return prom0;
	}
	
	public void testPromotionProcessChain() throws Exception {
        FreeStyleProject p1 = createFreeStyleProject();
        FreeStyleProject p2 = createFreeStyleProject();
        
        JobPropertyImpl promotion1 = new JobPropertyImpl(p1);
        p1.addProperty(promotion1);
        PromotionProcess promo1 = promotion1.addProcess("promo1");
        promo1.conditions.add(new SelfPromotionCondition(false));
        
        JobPropertyImpl promotion2 = new JobPropertyImpl(p1);
        p1.addProperty(promotion2);
        PromotionProcess promo2 = promotion2.addProcess("promo1");
        promo2.conditions.add(new SelfPromotionCondition(false));
        
	}
	
	public void testPromotionProcess() throws Exception {

        
	}
    public void testPromotionProcessViaWebClient() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        
        ExtensionList<Descriptor> list=Jenkins.getInstance().getExtensionList(Descriptor.class);
        list.add(new JobPropertyImpl.DescriptorImpl(JobPropertyImpl.class));
        JobPropertyImpl base =  new JobPropertyImpl(p);
        p.addProperty(base);
        createPromotionProcess(base, "PROM0");
        createPromotionProcess(base, "PROM1");
        createPromotionProcess(base, "PROM2");
        
        
        FreeStyleBuild b1 = assertBuildStatusSuccess(p.scheduleBuild2(0));
        assertNull(b1.getAction(ManualApproval.class));
        HtmlPage page=createWebClient().getPage(b1, "promotion");
        //Approve Promotion
        List<HtmlForm> forms=ManualConditionTest.getFormsByName(page, "approve");
        assertFalse(forms.isEmpty());
        assertEquals(3, forms.size());
        for (HtmlForm form:forms){
        	submit(form);
        }
        
        //reload promotions page
        page=createWebClient().getPage(b1, "promotion");
        forms=ManualConditionTest.getFormsByName(page,"build");
        for (HtmlForm form:forms){
        	List<HtmlElement> parameters=ManualConditionTest.getFormParameters(form);
        	assertEquals(2, parameters.size());
        }
    }
}
