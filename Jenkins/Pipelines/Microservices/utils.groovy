import jenkins.model.*
import hudson.model.*



import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl



def isFreeStyleJob(item){

  item instanceof hudson.model.FreeStyleProject
					
 }



/** def activateChuckNorrisOnAllJobs() {

	for(item in Jenkins.instance.items) {
			 println("job $item.name")
			 item.publishersList.replace(new  hudson.plugins.chucknorris.CordellWalkerRecorder());
	}
}**/
	
	
	
	
def activateChuckNorrisOneJob(job) {
	
	if (isFreestyleJob(job)==true) {
		
          item instanceof hudson.model.FreeStyleProject
                
		   
	}
}


 

/** Add a username password credential to the global credentials store **/
def add_project_un_credential(username, password) {

	domain = Domain.global()
	store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

   	usernameAndPassword = new UsernamePasswordCredentialsImpl(
	CredentialsScope.GLOBAL,
	password, "Jenkis Credential for Project Jobs with Username and Password Configuration",
	username,
	password
	)

	store.addCredentials(domain, usernameAndPassword)
}







