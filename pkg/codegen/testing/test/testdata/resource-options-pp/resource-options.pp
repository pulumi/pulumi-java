resource provider "pulumi:providers:aws" {
	region = "us-west-2"
}

resource bucket "aws:s3:Bucket" {
    website = {
    	indexDocument = "index.html"
    }

	options {
		provider = provider
		dependsOn = [provider]
		protect = true
	}
}

resource bucketWithoutArgs "aws:s3:Bucket" {
	options {
		provider = provider
		dependsOn = [provider]
		protect = true
		ignoreChanges = [bucket, lifecycleRules[0]]
	}
}
