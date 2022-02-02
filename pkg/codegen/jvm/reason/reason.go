package reason

import (
	"fmt"
)

// Describes why a file is being generated.
type FileGenReason string

// Build support files as settings.gradle, build.gradle.
func Gradle() FileGenReason {
	return "Gradle"
}

// Special Utilities.java file.
func Utilities() FileGenReason {
	return "Utilities"
}

// Special top-level README.md file.
func Readme() FileGenReason {
	return "Readme"
}

// Auto-generated Config.java module.
func Config() FileGenReason {
	return "Config"
}

// Files from `extraFiles` in `GeneratePackage`, passed by the user.
func ExtraFiles() FileGenReason {
	return "ExtraFiles"
}

// Decribes why a class is being generated.
type ClassGenReason string

func (r ClassGenReason) ToFileGenReason() FileGenReason {
	return FileGenReason(r)
}

// The class is generated to represent a resource.
func Resource(resourceToken string) ClassGenReason {
	return ClassGenReason(fmt.Sprintf("Resource(%s)", resourceToken))
}

// The class is generated to represent arguments to a resource.
func ResourceArgs(resourceToken string) ClassGenReason {
	return ClassGenReason(fmt.Sprintf("ResourceArgs(%s)", resourceToken))
}

// The class is generated to represent arguments to a GetFoo function
// accompanying a resource.
func ResourceGetArgs(resourceToken string) ClassGenReason {
	return ClassGenReason(fmt.Sprintf("ResourceGetArgs(%s)", resourceToken))
}

// The class is generated to represent a function (aka data source).
func Function(functionToken string) ClassGenReason {
	return ClassGenReason(fmt.Sprintf("Function(%s)", functionToken))
}

// The class is generated to represent arguments to a function.
func FunctionArgs(functionToken string) ClassGenReason {
	return ClassGenReason(fmt.Sprintf("FunctionArgs(%s)", functionToken))
}

// The class is generated to represent function result type.
func FunctionResult(functionToken string) ClassGenReason {
	return ClassGenReason(fmt.Sprintf("FunctionResult(%s)", functionToken))
}

type TypeVersion string

const (
	ArgsVersion   TypeVersion = "args"
	PlainVersion              = "plain"
	OutputVersion             = "output"
	StateVersion              = "state"
)

// The class is generated to represent a particular named type.
func Type(typeToken string, version TypeVersion) ClassGenReason {
	return ClassGenReason(fmt.Sprintf("Type(%s, %s)", string(version), typeToken))
}

// The class is generated to represent an enum.
func Enum(enumToken string) ClassGenReason {
	return ClassGenReason(fmt.Sprintf("Enum(%s)", enumToken))
}
