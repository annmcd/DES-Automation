

import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;

/** Signatures which can be auto authroized for use within Jenkins Pipeline**/
def sigList = ["field java.util.ArrayList size",
              "method hudson.model.DirectlyModifiableView add hudson.model.TopLevelItem", 
              "method hudson.model.Hudson getJob java.lang.String",
              "method hudson.model.Item getName",
              "method hudson.model.ItemGroup getItem java.lang.String",
              "method hudson.model.ItemGroup getItems",
              "method hudson.model.Job addProperty hudson.model.JobProperty",
              "method hudson.model.Job getLastBuild",
              "method hudson.model.Job getProperty java.lang.Class",
              "method hudson.model.Job getProperty java.lang.String",
              "method hudson.model.ModifiableViewGroup addView hudson.model.View",
              "method hudson.model.Run getNumber",
              "method hudson.model.Saveable save",
              "method hudson.model.View getItems",
              "method hudson.model.View getOwner",
              "method hudson.model.View getViewName",
              "method hudson.model.ViewGroup deleteView hudson.model.View",
              "method hudson.model.ViewGroup getView java.lang.String",
              "method hudson.model.ViewGroup getViews",
              "method java.lang.Class isInstance java.lang.Object",
              "method org.apache.maven.model.Model getGroupId",
              "method org.jenkinsci.plugins.workflow.support.actions.EnvironmentAction getEnvironment",
              "new hudson.model.ListView java.lang.String",
              "new hudson.model.ListView java.lang.String hudson.model.ViewGroup",
              "new hudson.model.ParametersDefinitionProperty java.util.List",
              "new hudson.model.StringParameterDefinition java.lang.String java.lang.String java.lang.String",
              "new java.lang.Exception java.lang.String",
              "new java.util.ArrayList",
              "staticMethod java.lang.System getenv java.lang.String",
              "staticMethod jenkins.model.Jenkins getInstance"]


// If the sigList contains signatures which are not already approved they are added and approved 
def authorisedSignatureCheck(signature) {
  
    def scriptApproval = ScriptApproval.get()
    def approvedSignatures = Arrays.asList(scriptApproval.approvedSignatures)
    
    if(approvedSignatures.contains(signature)){
      
        print ("Signature List already contains " + signature +"\n")
      
    }else{
        try{
            scriptApproval.pendingSignatures.add(new ScriptApproval.PendingSignature(signature, false, ApprovalContext.create()))
            scriptApproval.approveSignature(signature)
          
            if(Arrays.asList(scriptApproval.approvedSignatures).contains(signature)){
              
                print("Successfully added " + signature + "\n" )
            }else{
             	print ("Failed to add signature " + signature +"\n")
            }
        }catch(e){
          
          	print("Error evaluating/adding signature " +e)
        }
    }
}

  for (sig in sigList) {
    
      authorisedSignatureCheck(sig)
  }


