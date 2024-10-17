resource cluster "aws:ecs/cluster:Cluster" {
	__logicalName = "cluster"

	options {
		version = "5.16.2"
	}
}

resource lb "awsx:lb:ApplicationLoadBalancer" {
	__logicalName = "lb"

	options {
		version = "1.0.0-beta.5"
	}
}

resource nginx "awsx:ecs:FargateService" {
	__logicalName = "nginx"
	cluster = cluster.arn
	taskDefinitionArgs = {
		container = {
			image = "nginx:latest",
			cpu = 512,
			memory = 128,
			portMappings = [{
				containerPort = 80,
				targetGroup = lb.defaultTargetGroup
			}]
		}
	}

	options {
		version = "1.0.0-beta.5"
	}
}

output url {
	__logicalName = "url"
	value = lb.loadBalancer.dnsName
}
