mixed = invoke("enumlist:index:mix", {
    colors = ["red", "green"]
})

resource palette "enumlist:index:Palette" {
    primary = "red"
    colors = ["red", "green"]
    swatches = [{
        colors = ["green"]
    }]
}

output result {
    value = mixed.result
}
