zones = invoke("aws:index:getAvailabilityZones", {})

resource vpcSubnet "aws:ec2:Subnet" {
	options { range = zones.names }
	assignIpv6AddressOnCreation = false
	mapPublicIpOnLaunch = true
	cidrBlock = "10.100.${range.key}.0/24"
	availabilityZone = range.value
	tags = {
		"Name": "pulumi-sn-${range.value}"
	}
}