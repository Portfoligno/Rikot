[![Release](https://jitpack.io/v/io.github.justincase-jp/rikot.svg)](
  https://jitpack.io/#io.github.justincase-jp/rikot
)

Rikot
===
```
Hi {{you: Kotlin user}}!
```

Rikot is a typed template language designed for side-by-side usage with Kotlin.
Templates written in Rikot generates Kotlin objects, which could then be used in any Kotlin programs
with compile-time checks on each parameter.


## Syntax
Inline Comments
```
{# this will not be rendered in the output #}
```

Inline Variables
```
A variable 'foo' of type 'bar' has a value of {{foo: bar}} occasionally.
```

Inline Literals
```
{[Everything inside is automatically escaped.]}
{[Sequence of symbols like '{{' and '}}' will be retained.]}
{{[More braces may be used to retain sequences like ']}']}}
```

Full Line Comments
```
--# This whole line will be ignored #--
--#
  Useful for long, long comments. No need to mangle with new lines.
#--
```

Placeholder Types/Variables
```
--{foo: bar}--
--#
  'foo' will not be included in the output,
  but will still be represented as a parameter in the corresponding Kotlin object
#--

--{_: baz}--
--# Adds an extra type 'baz' to the corresponding Kotlin object #--
```

## User Code
```
--# Lorem.txt.rikot #--
Lorem ipsum dolor sit amet, {{consectetur: adipiscing}} elit, sed
do eiusmod tempor incididunt ut labore et dolore magna
aliqua. Ut {{enim: ad minim}} veniam, quis nostrud exercitation
ullamco laboris adipiscing nisi ut aliquip ex ea {{commodo: adipiscing}} consequat.
```

The above template can be called as such:
```kt
Lorem.txt(
  consectetur = "fugiat",
  enim = "qui",
  commodo = "officia"
)(
  adipiscing = ::Text,
  `ad minim` = ::Text
)
```


## Compiler Setup
* Include `compiler` dependencies:
```kt
buildscript {
  repositories {
    maven("https://jitpack.io")
  }
  dependencies {
    classpath("io.github.justincase-jp.rikot", "compiler", VERSION)
  }
}
```

* Invoke the compiler to generate Kotlin source code:
```kt
RikotCompiler().generateAll(File("src/main/rikot"), File("build/generated/rikot"))
```

* Configure Kotlin source sets for the generated source code.

* Include `runtime` dependencies:
```kt
repositories {
  jcenter()
  maven("https://jitpack.io")
}
dependencies {
  implementation("io.github.justincase-jp.rikot", "runtime", VERSION)
}
```

## Design Goals
The idiomatic way of doing structural generation in Kotlin is usually via the use of typed DSLs.
While being type-safe, there are limitations such as the inherent difficulty to provide a sandbox environment
for arbitrary inputs.

Rikot is designed to solve such problem by restricting possible constructs to literals and variables only,
while at the same time retaining type-safety and a certain level of flexibility.

As a template language, language constructs of Rikot are designed with conciseness and the ease of learning in mind.
For example, double brace syntax (```{{}}```) are chosen by this reason,
due to its popularity among templating languages.
