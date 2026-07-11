# shadepile transpiler

somewhat abstract transpiler but I tuned it for Lua

parts:

1. tokens ([lua.tokenize](src/underscore/andthereitgoes/shadepile/transpiler/lua/tokenize))
2. AST ([lua.parse](src/underscore/andthereitgoes/shadepile/transpiler/lua/parse))
3. emit ([lua.transpile](src/underscore/andthereitgoes/shadepile/transpiler/lua/transpile))
4. load ([lua.load](src/underscore/andthereitgoes/shadepile/transpiler/lua/load))
5. execute ([lua.runtime](src/underscore/andthereitgoes/shadepile/transpiler/lua/runtime))

oh also I'm trying to make this easily integrated directly into Java so I'm planning to have tools to convert an entire class into a Lua-accessible class at runtime
