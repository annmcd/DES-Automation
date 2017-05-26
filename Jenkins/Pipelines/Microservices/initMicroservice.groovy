

import jenkins.model.*



 Properties PROPS
 
 CUSTOM_WORKSPACE=""
 
  def readProps() {
 
	PROPS=new Properties()
	File propertiesFile
	println "workspace =${WORKSPACE}" 
	propertiesFile = new File("${WORKSPACE}" +"/Jenkins/Pipelines/Microservices/pipeline.properties")
	if (propertiesFile.exists()){
			propertiesFile.withInputStream{
				PROPS.load(it)
				}
				
		println "PropertyFileAccessTest= ${PROPS.lbl_pipeline_pr}"
		
	}
	else{
		println "Error: No pipeline.properties File Present"
		println System.getProperty("user.dir")
		return //1
		
	}
		
  }

      
  

  
  
  def setWorkspace() {
  
	jenkinsHome = Jenkins.instance.getRootDir().absolutePath.replaceAll("\\\\","/")
	CUSTOM_WORKSPACE =jenkinsHome +"/jobs/${ARTIFACT_ID}"
	println "CustomWorkspaceTest= ${CUSTOM_WORKSPACE}"
  }
  
 

	def abortBuild = { String abortMessage ->
    buildAborted = true
    error(abortMessage)
  } 
  
  
/*******Start of Job DSL Declare Microservice Job Configuration*******************/


  /**
   Job which sepcifies the SCM configuration for the repo
	master branch
  **/

  def createSCMJob(){
    
     job("${ARTIFACT_ID}${PROPS.lbl_scm}") {
      
	
	   publishers {
		 chucknorris()
		}
		
	  customWorkspace("${CUSTOM_WORKSPACE}")
      logRotator(28, 5, -1, -1)
      description("${PROPS.lbl_desc_scm}")
      label "${PROPS.targetSlave}"
      wrappers {
        preBuildCleanup();
		timestamps()
        timeout {
            noActivity(300);
        }
      }
      
       scm {
		git {
		  remote {
			url("${SCM_URL}")
		  }
		  branch('origin/master')

		  extensions {
			localBranch 'master'
		  }
       }
      }
    }
  }



   /**
   Job which sepcifies the SCM configuration for pull requests on the 
	repository
  **/

  def createSCMPRJob() {
  
      job("PullRequest-${ARTIFACT_ID}") {
        
          label "${PROPS.targetSlave}"
          description('Source code management for microservice PR build')
          logRotator(28, 10, -1, -1)
			publishers {
				chucknorris()
			}
          scm {
            git("${SCM_URL}","pr/*")
          }
        
          wrappers {
		  preBuildCleanup() 
		  timestamps()
		  
		  } 
          customWorkspace("${CUSTOM_WORKSPACE}")
		  
		
          
      }
  
  }


  
 
  

   /**
     Job which implements the steps which are executed as part of the
	 microservice pipeline on the master branch
    **/
  def createWorkerJob() {
  
	println "creating worker job ${PROPS.lbl_worker}"
  
	job("${ARTIFACT_ID}${PROPS.lbl_worker}") {
          
            parameters {
			
               stringParam('TARGET_STEP', '', 'Target param value indicating step execution')
            }
			 wrappers {
				timestamps()
			}   
						 
            //jdk("${jdkVersion}") 
            customWorkspace("${CUSTOM_WORKSPACE}")
            label "${PROPS.targetSlave}"
            description("${PROPS.lbl_desc_worker}")
            logRotator(28, 10, -1, -1)
                        
            steps {
                  conditionalSteps {
                      condition {
                        stringsMatch('${TARGET_STEP}', 
                                       "compile",true)
                   }
                    runner('Fail')
                      steps {

					    maven("clean compile -Pbugs-style-check  -Dmaven.test.skip=true -DfailFlag=${PROPS.const_fail_quality_check}")
        
                      }
                  }
             }
    
            steps {
                  conditionalSteps {
                    condition {
                      stringsMatch('${TARGET_STEP}', 
                                     "unit-tests",true)
                    }
                    runner('Fail')
					 
                    steps {
                    
						maven("test -DfailIfNoTests=${PROPS.const_fail_no_unit_tests} -Punit-tests -Dmax.classes.missed=${PROPS.const_unit_max_classes_missed} -Dmin.instruction.coverage=${PROPS.const_unit_target_percentage} ")
      
                    }
               }
           }
		   
           steps {
              conditionalSteps {
                  condition {
                    stringsMatch('${TARGET_STEP}', 
                                   "integration-tests",true)
                  }
                   runner('Fail')
                   steps {
				   
						maven("verify -DfailIfNoTests=${PROPS.const_fail_no_int_tests} -Pintegration-tests -Dmax.classes.missed=${PROPS.const_int_max_classes_missed} -Dmin.instruction.coverage=${PROPS.const_int_target_percentage} ")
    
                  }
             }
         }
		 
		 

		  steps {
              conditionalSteps {
                  condition {
                    stringsMatch('${TARGET_STEP}', 
                    "deploy-snapshot",true)
                  }
                   runner('Fail')
                   steps {
					
						maven("deploy -Pdeploy-snapshot -Dmaven.test.skip=true ")  //package microservice as jar with a custom manifest
					    
                  }
             }
         }
		 
		 
		 
		  steps {
				
              conditionalSteps {
                  condition {
                    stringsMatch('${TARGET_STEP}', 
                    "release-artefact",true)
                  }
                   runner('Fail')
                   steps { 
				    
						shell("git config --global user.email '${PROPS.service_account_email}' \n git config --global user.name '${PROPS.service_account_username}' ")

						maven("build-helper:parse-version  release:clean --batch-mode  release:prepare -Dtag=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}   -DreleaseVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion} -DdevelopmentVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}-SNAPSHOT -Dusername='${PROPS.service_account_logon}' -Dpassword='${PROPS.service_account_password}' -DdryRun=false -DskipTests=true")
					
						maven("-X release:perform")

                  }
             }
         }
		 
		 
		 
		steps {
            conditionalSteps {
                  condition {
                    stringsMatch('${TARGET_STEP}', 
                    "sonar",true)
                  }
                   runner('Fail')
                   steps {
					
					  maven("-X verify sonar:sonar") 
					    
                  }
             }
         }
		 
		 

		 publishers {

			chucknorris()
		}
		 
      }

  }




	//create a view to hang the jobs on
  def initialClassify() {
           
    listView("${PROPS.lbl_unclassified}${ARTIFACT_ID}") {
                  
                  
     description("View for all jobs related to microservice ${ARTIFACT_ID}")
     filterBuildQueue()
     filterExecutors()
     jobs{
         name("${ARTIFACT_ID}${PROPS.lbl_scm}")
         name("${ARTIFACT_ID}${PROPS.lbl_worker}")
         name("${ARTIFACT_ID}${PROPS.lbl_pipeline_main}")
         name("${ARTIFACT_ID}${PROPS.lbl_pipeline_pr}")
         }
         columns{
             status()
             weather()
             name()
             lastSuccess()
             lastFailure()
             lastDuration()
             buildButton()
             lastBuildConsole()
           }
         }
    }


 /** End of Job DSL **/





/*** Start of Groovy Pipeline for Microservices ***/


   /**
     Pipeline Job which makes calls to the microservice worker job
      in order to trigger each stage of the full pipeline
    **/
 def createMainPipeline() {
      
       pipelineJob("${ARTIFACT_ID}${PROPS.lbl_pipeline_main}") {
       description("${PROPS.lbl_desc_main_pipeline} ${ARTIFACT_ID}")
       logRotator(28, 10, -1, -1)
	  	   
       definition {
    
	   
          cps {
            sandbox()
            script("""
				/**** Begin Stage Methods *********/
				

				import jenkins.model.*
				
				 res=true		
                 //fail the pipeline if checkout fails
                 def doCheckout(){

					try{
						stage("${PROPS.lbl_checkout}") {
					  
						build "${ARTIFACT_ID}${PROPS.lbl_scm}"
					  }
				  }catch(e) {
					 	res=false
						//doSendEmail()
						currentBuild.result = "FAILURE"
					  
					  }
                    return res
                }
    
    
                // fail the pipeline if compile fails
                def doCompile() {

			
					try{
						stage("${PROPS.lbl_compile}") {
	   
						   build job: "${ARTIFACT_ID}${PROPS.lbl_worker}", 
						   parameters: [string(name: 'TARGET_STEP',
						   value: 'compile')]
					  
	 
						   def m = readMavenPom(file: '${CUSTOM_WORKSPACE}/pom.xml')
						   groupId = m.groupId
						   string artifactId = m.artifactId
												
							
						if (artifactId != "${ARTIFACT_ID}"){
							  res=false
							  echo "artifact in pom.xml is specified as: "
							  print artifactId
						
							  throw new Exception("Error: artifactId was entered as ${ARTIFACT_ID} it must match the artifactId in the projects pom.xml")
						 }
						
					    doClassify(groupId)
				
					    }
					  }catch(e){
					 	res=false
						currentBuild.result = "FAILURE"
					  }

					return res
                    
                }
            
                
                //Unit Test Conditions must pass
                def doUnitTests() {

		
					try {
					  stage("${PROPS.lbl_ut}") {
						build job: "${ARTIFACT_ID}${PROPS.lbl_worker}", 
						parameters: [string(name: 'TARGET_STEP',
						value: 'unit-tests')]
					  }  
					}catch(e) {
						res=false
						//doSendEmail()
						
						
					 }
					 
					 return res
                  }
    
    
    
                //Integration Test Conditions must pass
                def doIntegrationTests() {

             
                  try {
                      stage("${PROPS.lbl_it}") {
                     
                        build job: "${ARTIFACT_ID}${PROPS.lbl_worker}", 
                        parameters: [string(name: 'TARGET_STEP',
                        value: 'integration-tests')]
                       }  
                  }catch(e) {
                        res=false
						currentBuild.result = "FAILURE"
  						//doSendEmail()
						
                  }
			
					return res
				
			    }
    
				
    
                def doPublishReports() {
    
                   try {
				
                        stage("${PROPS.lbl_publish}") {
    
                          if (fileExists("${CUSTOM_WORKSPACE}/target/site/jacoco-ut")) {
                             echo "About to publish unit test report"
                             publishHTML target: [
                             allowMissing: false,
                             alwaysLinkToLastBuild: false,
                             keepAll: true,
                             reportDir: "${CUSTOM_WORKSPACE}/target/site/jacoco-ut",
                             reportFiles: 'index.html',
                             reportName: 'Unit-Test-Report'
                             ]
                          }
    
                           if (fileExists("${CUSTOM_WORKSPACE}/target/site/jacoco-it")) {
                             echo "About to publish integration test report"
                             publishHTML target: [
                             allowMissing: false,
                             alwaysLinkToLastBuild: false,
                             keepAll: true,
                             reportDir: "${CUSTOM_WORKSPACE}/target/site/jacoco-it",
                             reportFiles: 'index.html',
                             reportName: 'Integration-Test-Report'
                             ]
                          }
                    
                        }
                      }catch (e) {
							res=false
							//doSendEmail()
							currentBuild.result = "FAILURE"
							
                      }
                     
					 return res
                }
				
				
				
    
				//fail pipeline on failure of step
                def doSnapshotDeploy() {
				
					try{
    
						stage("${PROPS.lbl_snapshot_deploy}") {
			
							echo "About to package artefact & deploy as a snapshot"
							build job: "${ARTIFACT_ID}${PROPS.lbl_worker}", 
										parameters: [string(name: 'TARGET_STEP',
										value: 'deploy-snapshot')]
						 }	
					}catch(e){
					
					    res=false
                        //doSendEmail()
						currentBuild.result = "FAILURE"
						
					}
					
					return res
                }
                

				
				
				
                def doDockerCompose() {
    
					try{ 
					    stage ("${PROPS.lbl_docker_compose}") {
							echo "aout to create image of env"
					   }	
					 }catch(e) {
						res=false
						//doSendEmail()
						 currentBuild.result = "FAILURE"

					}	
					
					return res					
                }

    
	
	
	
                def doProvisionEnv() {
    
					stage ("${PROPS.lbl_provision_environment}") {
    
                      echo "About to provision microservice test environment "
					}		
                }
    

	
	
	
                def doRunAcceptanceTests() {
				
					try{ 
	
						stage ("${PROPS.lbl_run_at}") {

							echo "About to run Acceptance Tests "
						}		
					  }catch(e){
						res=false
                        // doSendEmail()
					   currentBuild.result = "FAILURE"
					  }
					  
					  return res
                }
                

				
				
				
                def doArtifactRelease(){
    
					try{ 
						stage ("${PROPS.lbl_artefact_release}") {
		
						  echo "About to release artefact"

						  build job: "${ARTIFACT_ID}${PROPS.lbl_worker}", 
							parameters: [string(name: 'TARGET_STEP',
							value: 'release-artefact')]
						}	
					}catch(e) {
						res=false
                      // doSendEmail()
					  currentBuild.result = "FAILURE"
					  
					}
					return res
                }
				
				
				
				
				
				def doPrune() {
				
					stage ("${PROPS.lbl_artefact_prune}") {
					
						echo "About to prune artefact"
					}
				}
    
	
	
    
                def doChefPush(){
    
					stage ("${PROPS.lbl_chef_push}") {
		
						echo "About to push to CHEF Server"
					 }		
                 
                }
    
	
                def doNotifyStash(){
    
                   try {
                        stage ("${PROPS.lbl_stash}") {
							echo "About to notify stash"
                        }		
                             
                    }catch (e) {
                        res=false
						//doSendEmail()
						currentBuild.result = "FAILURE"
                  }
				  return res
                }
    
    
	
	
	
                def doSonarPush() {
				
				try{ 
    
                    stage ("${PROPS.lbl_sonar}") {
                      
					echo "About to push to sonar"
                     build job: "${ARTIFACT_ID}${PROPS.lbl_worker}", 
						parameters: [string(name: 'TARGET_STEP',
						value: 'sonar')]
					}		
				  
				  }catch(e) {
					 res=false
                    // doSendEmail()
					currentBuild.result = "FAILURE"
				  }
				  return res
				  
                }	



	       




	       /** Until this generated pipeline is triggered, we do not know the groupId of the project
			Therefore we must classify the jobs related to each microservice post execution of the pipeline
			and add them to a view named with the maven groupId of the project
	      **/
		def getUnclassifiedView(){

			def ucView=null
			try{
				println "determining if view exists:  ${PROPS.lbl_unclassified}${ARTIFACT_ID}"
				 
				//ucView= hudson.model.Hudson.getInstance().getView("${PROPS.lbl_unclassified}${ARTIFACT_ID}")
			    ucView=Jenkins.instance.getView("${PROPS.lbl_unclassified}${ARTIFACT_ID}")
				
				}catch(e) {
				  throw e
				  currentBuild.result = "FAILURE"
				}
           	 return ucView
         }



	
	     def getClassifiedView(groupId){

             def classifiedView=null
			 try{
             //classifiedView= hudson.model.Hudson.getInstance().getView(groupId)
             classifiedView = Jenkins.instance.getView(groupId)
			 
			 }catch(e) {
				throw e
				currentBuild.result = "FAILURE"
			 }
              return classifiedView
          }




	     def doClassify(groupId) {
		

			def ucView=getUnclassifiedView()
			println "determining if classified view exists"
			def classifiedView=getClassifiedView(groupId)
			def bNoViews=false
			
			
			//if classified view does not exist then create it
			if(classifiedView ==null){ 
				 println "classified view does not exist for the groupId,  create it"
	  
				   def view = new ListView(groupId, Jenkins.instance)
				   Jenkins.instance.addView(view)
				   Jenkins.instance.save()    
			  }

			//If the unclassified view exists add the jobs to the classified view and delete it
			if (ucView != null) {
				 println "unclassified view scheduled for deletion, jobs will be moved to the classified view, represented by the project groupId."
			   
				  //copy all projects of a view
				  for(item in ucView.getItems() ) {
							
					def jobItem=item
					print jobItem.name
					Job job = Jenkins.instance.getItem(jobItem.name);
					classifiedView=getClassifiedView(groupId)
					classifiedView.add(job)
					
				  }
				  println "delete the unclassified view... "
				  def vc = ucView.getOwner();
				  vc.deleteView(ucView)
				  println "....view deleted"
			 }

	     }

      

		def doSendEmail() {
        
              stage("SendEmail") {

				mail from: "${PROPS.emailSender}", 
                to: "${EMAIL_DISTRIBUTION_LIST}",
                subject: "Job Failure: "+ env.JOB_NAME,
                mimeType: "text/html",
                body: "<b>Job Name:</b> " + env.JOB_NAME +  "<b> Build Number:</b>[ " + env.BUILD_NUMBER + "]<br><b>For Full Details See Console Output For: </b>" + env.BUILD_URL

           	 }
         }

		 


		
	  //Entry Point for Main Mircroservice Pipeline
	   node("${PROPS.targetSlave}") {
				
	      doCheckout()
	  	  doCompile()
	      resultUT = doUnitTests()
	      resultIT = doIntegrationTests()
	      doPublishReports() //publish reports regardless
	    

	     if (resultUT==true && resultIT==true) {
			  doSnapshotDeploy()
			  doDockerCompose()
			  doProvisionEnv()
			  doRunAcceptanceTests()
			  doArtifactRelease()
			  doPrune()
			  doChefPush()
			  doNotifyStash()
		  }else{
			 prinltn "Both Integration and Unit Tests must succced"
			 currentBuild.result = "FAILURE"
			 //doSendEmail
			 
		  }
			  doSonarPush()
			  	    
       }   
          """.stripIndent())      
          }
        }
	 
    }
  }


	/**Seed Jobs Entry point**/
	readProps()
	setWorkspace()
	createSCMJob()
	createWorkerJob()
	//createSCMPRJob()
	createMainPipeline()
	initialClassify()