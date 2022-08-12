// Copyright 2022, Pulumi Corporation.  All rights reserved.

package names

import "testing"

func TestMakeSafeEnumName(t *testing.T) {
	tests := []struct {
		input    string
		expected string
		wantErr  bool
	}{
		{"+", "", true},
		{"*", "Asterisk", false},
		{"0", "Zero", false},
		{"8.3", "_8_3", false},
		{"11", "_11", false},
		{"Microsoft-Windows-Shell-Startup", "MicrosoftWindowsShellStartup", false},
		{"Microsoft.Batch", "Microsoft_Batch", false},
		{"final", "Final_", false},
		{"SystemAssigned, UserAssigned", "SystemAssigned_UserAssigned", false},
		{"Dev(NoSLA)_Standard_D11_v2", "Dev_NoSLA_Standard_D11_v2", false},
		{"Standard_E8as_v4+1TB_PS", "Standard_E8as_v4_1TB_PS", false},
	}
	for _, tt := range tests {
		t.Run(tt.input, func(t *testing.T) {
			got, err := MakeSafeEnumName(tt.input, "TypeName")
			if (err != nil) != tt.wantErr {
				t.Errorf("makeSafeEnumName() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.expected {
				t.Errorf("makeSafeEnumName() got = %v, want %v", got, tt.expected)
			}
		})
	}
}

func TestValidIdent(t *testing.T) {
	tests := []struct {
		input    string
		expected string
	}{
		{"$default", "$default"},
		{"default", "default_"},
		{"@default", "_default"},
		{"8", "_8"},
		{"azure-native", "azurenative"},
		{"package", "package_"},
		{"foo", "foo"},
	}
	for _, tt := range tests {
		t.Run(tt.input, func(t *testing.T) {
			if got := Ident(tt.input).String(); got != tt.expected {
				t.Errorf("Ident.String() = %v, want %v", got, tt.expected)
			}
		})
	}
}

func TestProperty_Getter(t *testing.T) {
	tests := []struct {
		input    string
		expected string
	}{
		{"JSONPath", "JSONPath"},
		{"notify", "notify_"},
		{"wait", "wait_"},
		{"builder", "builder_"},
	}
	for _, tt := range tests {
		t.Run(tt.input, func(t *testing.T) {
			if got := Ident(tt.input).AsProperty().Getter(); got != tt.expected {
				t.Errorf("Ident.AsProperty().Getter() = %v, want %v", got, tt.expected)
			}
		})
	}
}

func TestProperty_ResourceGetter(t *testing.T) {
	tests := []struct {
		input    string
		expected string
	}{
		{"JSONPath", "JSONPath"},
		{"notify", "notify_"},
		{"wait", "wait_"},
		{"builder", "builder_"},
		{"getId", "getId_"},
		{"getResourceName", "getResourceName_"},
	}
	for _, tt := range tests {
		t.Run(tt.input, func(t *testing.T) {
			if got := Ident(tt.input).AsProperty().ResourceGetter(); got != tt.expected {
				t.Errorf("Ident.AsProperty().ResourceGetter() = %v, want %v", got, tt.expected)
			}
		})
	}
}
