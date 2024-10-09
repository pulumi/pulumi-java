config instanceType string {
	__logicalName = "InstanceType"
	default = "t3.micro"
}

ec2Ami = invoke("aws:ec2/getAmi:getAmi", {
	filters = [{
		name = "name",
		values = ["amzn2-ami-hvm-2.0.20231218.0-x86_64-ebs"]
	}],
	owners = ["137112412989"],
	mostRecent = true
}).id

resource webSecGrp "aws:ec2/securityGroup:SecurityGroup" {
	__logicalName = "WebSecGrp"
	ingress = [{
		protocol = "tcp",
		fromPort = 80,
		toPort = 80,
		cidrBlocks = ["0.0.0.0/0"]
	}]

	options {
		version = "5.16.2"
	}
}

resource webServer "aws:ec2/instance:Instance" {
	__logicalName = "WebServer"
	instanceType = instanceType
	ami = ec2Ami
	userData = "#!/bin/bash\necho 'Hello, World from ${webSecGrp.arn}!' > index.html\nnohup python -m SimpleHTTPServer 80 &"
	vpcSecurityGroupIds = [webSecGrp.id]

	options {
		version = "5.16.2"
	}
}

resource defaultProvider "pulumi:providers:aws" {
	__logicalName = "DefaultProvider"

	options {
		version = "5.16.2"
	}
}

resource usEast2Provider "pulumi:providers:aws" {
	__logicalName = "UsEast2Provider"
	region = "us-east-2"

	options {
		version = "5.16.2"
	}
}

resource myBucket "aws:s3/bucket:Bucket" {
	__logicalName = "MyBucket"

	options {
		version = "5.16.2"
	}
}

output instanceId {
	__logicalName = "InstanceId"
	value = webServer.id
}

output publicIp {
	__logicalName = "PublicIp"
	value = webServer.publicIp
}

output publicHostName {
	__logicalName = "PublicHostName"
	value = webServer.publicDns
}
