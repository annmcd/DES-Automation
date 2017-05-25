


	//list view names
	def getViews() {

		Jenkins.instance.views.each { 
		  view ->
		  println "${view.name}"
		  view.items.each {
			item ->
			println "\t- ${item.name}"
		  }
		}
	}

