import groovy.json.JsonBuilder
import org.codehaus.groovy.runtime.MethodClosure

include 'asynchttp_v1'

def getNAMESPACE() { return "zabracks" }
def getPARENT_NAME() { return "MyRE" }
def getAPP_NAME() { return "${PARENT_NAME}-worker" }
def getVERSION() { return "0.0.1" }

definition(
        name: APP_NAME,
        namespace: NAMESPACE,
        author: "Drew Worthey",
        description: "MyRE worker app. Do not install directly.",
        parent: "zabracks:${PARENT_NAME}",
        category: "Convenience",
        singleInstance: false
)

preferences {
    page(name: "pageInit")
}

def pageInit() {
    return dynamicPage(name: "pageInit", title: "", uninstall: !!state.inited) {
        if(parent && parent.isInstalled()) {
            section() {
                paragraph "This is the worker app for the ${state.name} project."
            }
        } else {
            section() {
                paragraph "You cannot install this SmartApp directly. You must install the ${PARENT_NAME} app first."
            }
        }
    }
}

def getAllDevices() {
    return parent.getManagedDevices()
}

def getDeviceById(deviceId) {
    return parent.getDeviceStatusById(deviceId)
}

///// Implementation of MyreLisp intepreter
// I'm adapting much of MAL (https://github.com/kanaka/mal/blob/master/groovy) for this.
// Due to limitations of the SmartThings API, we're forced to do things in a hacky way.

/// Types
// Since ST doesn't allow class definitions, we're going to do everything with functions and maps...
// for better or for worse.
def getUriForEndpoint(path) {
    return parent.getConsoleBaseUrl() + path
}

def handleLogResponse(response, data) {
    log.info(response.data)
}

public String mem(showBytes = true) {
    def bytes = toJson(state).length()
    return Math.round(100.00 * (bytes/ 100000.00)) + "%${showBytes ? " ($bytes bytes)" : ""}"
}

def sendProjectLog(String projectId, Map logData) {
    def uri = getUriForEndpoint("api/Projects/$projectId/logs")
    def params = [
            uri: uri,
            body: logData
    ]

    try {
        asynchttp_v1.post(handleLogResponse, params)
    } catch (ex) {
        log.error("Error while attempting to do the things.", ex)
    }
}

def mlog(s, String level="info", context=[:]) {
    sendProjectLog(state.projectId, [
            message: s,
            level: level,
            context: context,
            bytesUsed: mem()
    ])
}

String toBase64(s) {
    // If this doesn't work, look here: https://community.smartthings.com/t/posting-emails-w-base64-attachments-from-smartapps/5668/3
    s.bytes.encodeBase64()
}

String toJson(o) {
    (new groovy.json.JsonBuilder(o)).toString()
}

Map fromJson(s) {
    parseJson(s)
}

def deepcopy(orig) {
    def result = fromJson(toJson(orig))
    return result
}

Map MyreLispException(Object msg) {
    mlog(msg, "error")
    return [ t: "EX", message: msg ]
}

Map Symbol(name) {
    [ t: "SYMBOL", value: name ]
}
def symbol_Q(o) {
    return o instanceof Map && o.get("t", "") == "SYMBOL"
}

int Symbol__compare(a, b) {
    a['value'] <=> b['value']
}

Map Atom(value) {
    [ t: 'ATOM', value: value ]
}
def atom_Q(o) {
    return o instanceof Map && o.get("t", "") == "ATOM"
}

Map Function(_EVAL, _ast, _env, _params) {
    [
            t: 'FUNC',
            //EVAL: _EVAL,
            ast: _ast,
            env: deepcopy(_env),
            params: _params,
            ismacro: false
    ]
}
def function_Q(o) {
    return o instanceof Map && o.get("t", "") == "FUNC"
}
def Function__call(func, args) {
        def new_env = Env(func['env'], func['params'], args)
        try {
            return EVAL(func['ast'], new_env)
        } catch (Exception ex) {
            mlog(ex, "error")
            return MyreLispException(ex)
        }
}

def GlobalClosureReference(name) {
    [
            t: 'GFUNC',
            name: name
    ]
}

def globalClosure_Q(o) {
    return o instanceof Map && o.get("t", "") == "GFUNC"
}

def GlobalClosureReference__call(gfunc, args) {
    Map ns = getPrimaryNamespace()
    def closure = ns.get(gfunc['name'], null)
    if(!closure) {
        return MyreException("Attempted to call non-existent global reference ${gfunc['name']}")
    } else {
        return closure(args)
    }
}

def DeviceReference(devId) {
    return [
        t: "DEVREF",
        devId: devId
    ]
}

def device_Q(d) {
    d instanceof Map && d.get("t", "") == "DEVREF"
}

def DeviceReference__get(dref) {
    def devices = getAllDevices()
    def d = devices.find({ dref["devId"] == it.id })
    if(d) {
        return d
    } else {
        log.error("failed to retrieve device ${dref}")
        return MyreLispException("Device with identifier '${dref}' not found.")
    }
}

def DeviceReference__fromIdentifier(identifier) {
    def devices = getAllDevices()
    def d = devices.find({ identifier == it.id || identifier == it.label })
    if(d) {
        return DeviceReference(d.id)
    } else {
        log.error("failed to retrieve device ${dref}")
        return MyreLispException("Device with identifier '${dref}' not found.")
    }
}

def string_Q(o) {
    return o instanceof String && (o.size() == 0 || o[0] != "\u029e")
}

def keyword_Q(o) {
    return o instanceof String && o.size() > 0 && o[0] == "\u029e"
}
def keyword(o) {
    this.&keyword_Q(o) ? o : ("\u029e" + o)
}

def list_Q(o) {
    return o instanceof List && !(o instanceof LinkedList)
}

def vector(o) {
    def v = o.collect() as LinkedList
    v
}
def vector_Q(o) {
    return o instanceof LinkedList
}
def hash_map(lst) {
    def m = [:]
    assoc_BANG(m, lst)
}
def assoc_BANG(m, kvs) {
    for (int i=0; i<kvs.size(); i+=2) {
        m[kvs[i]] = kvs[i+1];
    }
    return m
}
def dissoc_BANG(m, ks) {
    for (int i=0; i<ks.size(); i++) {
        m.remove(ks[i])
    }
    return m
}
def hash_map_Q(o) {
    return o instanceof Map && o.get("t", null) == null
}

def sequential_Q(o) {
    return list_Q(o) || vector_Q(o)
}

/// Env
Map Env(Map outer_env = null, binds = null, exprs = null) {
    if(!binds) binds = []
    if(!exprs) exprs = []
    def result = [:]
    result['outer'] = outer_env
    result['data'] = [:]
    for (int i=0; i<binds.size; i++) {
        if (binds[i].value == "&") {
            result['data'][binds[i+1]['value']] = (exprs.size() > i) ? exprs[i..-1] : []
            break
        } else {
            result['data'][binds[i]['value']] = exprs[i]
        }
    }
    result
}

def Env__set(Map env, Map keySymbol, val) {
    try {
        return env['data'][keySymbol['value']] = val
    } catch (e) {
        mlog(e, "error", env)
    }
}

def Env__find(Map env, Map keySymbol, int findDepth = 0) {
    if(env['data'].containsKey(keySymbol['value'])) {
        return env
    } else if(env['outer'] != null) {
        def result = Env__find(env['outer'], keySymbol, (findDepth + 1))
        return result
    } else {
        Map pns = getPrimaryNamespace()
        if(pns.containsKey(keySymbol["value"])) {
            mlog("found builtin ${keySymbol["value"]}")
            return Env(null, [keySymbol], [pns[keySymbol['value']]])
        }

        return null
    }
}
def Env__get(Map env, Map keySymbol) {
    def e = Env__find(env, keySymbol)
    if(!e) {
        return MyreLispException("Failed to find '${keySymbol['value']}' in ${env}")
    } else {
        e['data'].get(keySymbol['value'])
    }
}

def Env__getRoot(Map env) {
    if(env["outer"] != null) return Env__getRoot(env["outer"])
    return env["outer"]
}

/// Printer
def print_list(lst, sep, Boolean print_readably) {
    return lst.collect{ e -> pr_str(e, print_readably) }.join(sep)
}
def pr_str(exp, Boolean print_readably) {
    def _r = print_readably
    switch (exp) {
        case { list_Q(exp) }:
            def lst = exp.collect { pr_str(it, _r) }
            return "(${lst.join(" ")})"
        case { vector_Q(exp) }:
            def lst = exp.collect { pr_str(it, _r) }
            return "[${lst.join(" ")}]"
        case { symbol_Q(exp) }:
            return exp['value']
        case { atom_Q(exp) }:
            return "(atom ${exp['value']})"
        case Map:
            def lst = []
            exp.each { k,v -> lst.add(pr_str(k,_r)); lst.add(pr_str(v,_r)) }
            return "{${lst.join(" ")}}"
        case String:
            if (keyword_Q(exp)) {
                return ":" + exp.drop(1)
            } else if (print_readably) {
                //return "\"${StringEscapeUtils.escapeJava(exp)}\""
                return exp
            } else {
                return exp
            }
        case MethodClosure:
            return "MethodClosure"
        case Closure:
            return "Closure"
        case null:
            return 'nil'
        default:
            return exp.toString()
    }
}

/// Reader
Map Reader(tokens) {
    [ t: 'READER', tokens: tokens, position: 0 ]
}
def Reader__peek(Map reader) {
    if (reader['position'] >= reader['tokens'].size) {
        return null
    } else {
        return reader['tokens'][reader['position']]
    }
}
def Reader__next(reader) {
    if (reader['position'] >= reader['tokens'].size) {
        null
    } else {
        def nextToken = reader['tokens'][reader['position']]
        reader['position'] = reader['position'] + 1
        nextToken
    }
}
def tokenizer(String str) {
    def mask = /[\s,]*(~@|[\[\]{}()'`~^@]|"(?:\\.|[^\\"])*"|;.*|[^\s\[\]{}('"`,;)]*)/
    def m = str =~ mask

    def tokens = []
    for(item in m) {
        String token = item[1]
        if (token != null &&
                !(token == "") &&
                !(token[0] == ';')) {
            tokens.add(token)
        }
    }
    return tokens
}
def read_atom(Map rdr) {
    String token = Reader__next(rdr)

    def mask = /(^-?[0-9]+$)|(^-?[0-9][0-9.]*$)|(^nil$)|(^true$)|(^false$)|^"(.*)"$|:(.*)|(^[^"]*$)/
    def m = (token =~ mask)[0]

    if (m[1] != null) {
        Integer.parseInt(m[1])
    } else if (m[3] != null) {
        null
    } else if (m[4] != null) {
        true
    } else if (m[5] != null) {
        false
    } else if (m[6] != null) {
        //StringEscapeUtils.unescapeJava(m.group(6))
        m[6]
    } else if (m[7] != null) {
        "\u029e" + m[7]
    } else if (m[8] != null) {
        Symbol(m[8])
    } else {
        return MyreLispException("unrecognized '${m.group(0)}' at position ${rdr['position']}. Instead got ${token}.")
    }
}
def read_list(Map rdr, char start, char end) {
    String token = Reader__next(rdr)
    def lst = []
    if (token.charAt(0) != start) {
        return MyreLispException("expected '${start}' at position ${rdr['position']}. Instead got ${token}.")
    }

    while ((token = Reader__peek(rdr)) != null && token.charAt(0) != end) {
        lst.add(read_form(rdr))
    }

    if (token == null) {
        return MyreLispException("expected '${end}', got EOF at position ${rdr['position']}. Instead got ${token}.")
    }
    Reader__next(rdr)

    return lst
}
def read_vector(Map rdr) {
    def lst = read_list(rdr, '[' as char, ']' as char)
    return vector(lst)
}

def read_hash_map(Map rdr) {
    def lst = read_list(rdr, '{' as char, '}' as char)
    return hash_map(lst)
}

def read_form(Map rdr) {
    def token = Reader__peek(rdr)
    switch (token) {
    // reader macros/transforms
        case "'":
            Reader__next(rdr)
            return [Symbol("quote"), read_form(rdr)]
        case '`':
            Reader__next(rdr)
            return [Symbol("quasiquote"), read_form(rdr)]
        case '~':
            Reader__next(rdr)
            return [Symbol("unquote"), read_form(rdr)]
        case '~@':
            Reader__next(rdr)
            return [Symbol("splice-unquote"), read_form(rdr)]
        case '^':
            Reader__next(rdr)
            def meta = read_form(rdr)
            return [Symbol("with-meta"), read_form(rdr), meta]
        case '@':
            Reader__next(rdr)
            return [Symbol("deref"), read_form(rdr)]

    // list
        case ')': return MyreLispException("unexpected ')' at position ${rdr['position']}")
        case '(': return read_list(rdr, '(' as char, ')' as char)

    // vector
        case ']': return MyreLispException("unexpected ']' at position ${rdr['position']}")
        case '[': return read_vector(rdr)

    // hash-map
        case '}': return MyreLispException("unexpected '}' at position ${rdr['position']}")
        case '{': return read_hash_map(rdr)

    // atom
        default: return read_atom(rdr)
    }
}

def read_str(String str) {
    def tokens = tokenizer(str)
    if (tokens.size() == 0) {
        return null
    }
    def rdr = Reader(tokens)
    read_form(rdr)
}

/// Core

def tryGetProperty(o, propName) {
    try {
        o."${propName}"
    } catch (Exception e) {
        null
    }
}

def tryGetMethod(o, methodName) {
    try {
        o.&"${methodName}"
    } catch (e) {
        null
    }
}

def getMember(o, memberName) {
    if(o instanceof Map) {
        if(device_Q(o)) {
            return getMember(DeviceReference__get(o), memberName)
        } else {
            o[memberName]
        }
    } else {
        def m = tryGetProperty(o, memberName)
        if(m != null) {
            return m
        }
        m = tryGetMethod(o, memberName)
        if(m != null) {
            return m
        }
        o."${memberName}"
    }
}

Map getPrimaryNamespace() {
    [
            "=": { a -> a[0]==a[1]},
            "throw": { a -> return MyreLispException(a[0]) },

            "nil?": { a -> a[0] == null },
            "true?": { a -> a[0] == true },
            "false?": { a -> a[0] == false },
            "string?": { a -> string_Q(a[0]) },
            "symbol": { a -> Symbol(a[0]) },
            "symbol?": { a -> a[0] instanceof Map && a[0]['t'] == "SYMBOL"},
            "keyword": { a -> keyword(a[0]) },
            "keyword?": { a -> keyword_Q(a[0]) },
            "number?": { a -> a[0] instanceof Integer },
            "fn?": { a -> (a[0] instanceof Map && a[0]['t'] == "FUNC" && !a[0]['ismacro']) ||
                    a[0] instanceof Closure },
            "macro?": { a -> a[0] instanceof Map && a[0]["t"] == "FUNC" && a[0]['ismacro'] },

            "pr-str": { a -> print_list(a, " ", true)},
            "str": { a -> print_list(a, "", false)},
            "prn": { a -> mlog(print_list(a, " ", true), "info")},
            "println": { a -> mlog(print_list(a, " ", false), "info")},
            "read-string": this.&read_str,

            "<":  { a -> a[0]<a[1]},
            "<=": { a -> a[0]<=a[1]},
            ">":  { a -> a[0]>a[1]},
            ">=": { a -> a[0]>=a[1]},
            "+":  { a -> a[0]+a[1]},
            "-":  { a -> a[0]-a[1]},
            "*":  { a -> a[0]*a[1]},
            "/":  { a -> a[0]/a[1]},  // /
            "time-ms": { a -> System.currentTimeMillis() },

            "list": { a -> a },
            "list?": { a -> list_Q(a[0]) },
            "vector": { a -> vector(a) },
            "vector?": { a -> vector_Q(a[0]) },
            "hash-map": { a -> hash_map(a) },
            "map?": { a -> hash_map_Q(a[0]) },
            "assoc": { a -> assoc_BANG(types.copy(a[0]), a.drop(1)) },
            "dissoc": { a -> dissoc_BANG(types.copy(a[0]), a.drop(1)) },
            "get": { a ->
                log.info("get called with args: ${a}")
                def k
                if(symbol_Q(a[1])) {
                    k = a[1]['value']
                } else if (string_Q(a[1])) {
                    k = a[1]
                }

                if(a[0] instanceof Map && !device_Q(a[0])) {
                    a[0] == null ? null : a[0][a[1]]
                } else {
                    getMember(a[0], a[1])
                }
            },
            "contains?": { a -> a[0].containsKey(a[1]) },
            "keys": { a -> a[0].keySet() as List },
            "vals": { a -> a[0].values() as List },

            "sequential?": { a -> this.&sequential_Q(a[0]) },
            "cons": { a -> [a[0]] + (a[1] as List) },
            "concat": { args -> args.inject([], { a, b -> a + (b as List) }) },
            "nth": {args ->
                if (args[0].size() <= args[1]) {
                    return MyreLispException("nth: index out of range at position ${rdr['position']}")
                }
                args[0][args[1]]
            },
            "first": { a -> a[0] == null || a[0].size() == 0 ? null : a[0][0] },
            "rest": { a -> a[0] == null ? [] as List : a[0].drop(1) },
            "empty?": { a -> a[0] == null || a[0].size() == 0 },
            "count": { a -> a[0] == null ? 0 : a[0].size() },
            "apply": {args ->
                def start_args = args.drop(1).take(args.size()-2) as List
                args[0](start_args + (args.last() as List))
            },
            "filter": { a -> a[1].findAll { x -> Function__call(a[0], [x]) } },
            "map": { a -> a[1].collect { x -> Function__call(a[0], [x]) } },

            "conj": { args ->
                if (types.list_Q(args[0])) {
                    args.drop(1).inject(args[0], { a, b -> [b] + a })
                } else {
                    vector(args.drop(1).inject(args[0], { a, b -> a + [b] }))
                }
            },
            "seq": { args ->
                def obj = args[0]
                switch (obj) {
                    case { list_Q(obj) }:
                        return obj.size() == 0 ? null : obj
                    case { vector_Q(obj) }:
                        return obj.size() == 0 ? null : obj.clone()
                    case { string_Q(obj) }:
                        return obj.size() == 0 ? null : obj.collect{ it.toString() }
                    case null:
                        return null
                    default:
                        return MyreLispException("seq: called on non-sequence at position ${rdr['position']}")
                }
            },

            // TODO: figure out another implementation of meta and with-meta
            //"meta": { a -> a[0].hasProperty("meta") ? a[0].getProperties().meta : null },
            //"with-meta": { a -> def b = types.copy(a[0]); b.getMetaClass().meta = a[1]; b },
            "atom": { a -> Atom(a[0]) },
            "atom?": { a -> a[0] instanceof Map && a[0]["t"] == "ATOM"},
            "deref": { a -> a[0]['value'] },
            "reset": { a -> a[0]['value'] = a[1] },
            "swap": { args ->
                def atm = args[0]
                def f = args[1]
                atm['value'] = f([atm.value] + (args.drop(2) as List))
            },

            "get-devices": { a -> vector(getAllDevices().collect { DeviceReference(it.id) }) },
            "dev": { a -> DeviceReference__fromIdentifier(a[0]) },
            "dev-cmd": { a ->
                // (dev-cmd (dev "Button") "on" [])         device command
                // (dev-cmd (dev "Switch") "toggle" [])     virtual command
                def devRef = a[0]
                def d = DeviceReference__get(devRef)

                def command = a[1]
                def params = a[2]

                mlog("Attempting to execute command '${d.label}'::'$command'")

                // TODO: Determine if it's a virtual command or a legit smartthings command

                if(params == null) {
                    mlog("Parameters were not provided when calling device command: $command", "error")
                    params = []
                }

                if(params.size()) {
                    d."$command"(params as Object[])
                } else {
                    d."$command"()
                }

                return null
            },
            "dev-attr": { a ->
                // (dev-attr (dev "button") "switch")
                def devRef = a[0]
                def attrName = a[1]

                def d = DeviceReference__get(devRef)
                return d."current$attrName"
            }
    ]
}

def getCoreEnv() {
    def e = Env()
    getPrimaryNamespace().each {
        k, v ->
            Env__set(e, Symbol(k), GlobalClosureReference(k))
    }
    e
}

/// REPL
def READ(str) {
    def result = read_str(str)
    result
}

def macro_Q(ast, env) {
    if (list_Q(ast) &&
            ast.size() > 0 &&
            ast[0] instanceof Map &&
            ast[0]['t'] == "SYMBOL") {
        def eexist = Env__find(env, ast[0])
        if(!eexist) {
            return false
        }
        def obj = Env__get(eexist, ast[0])
        if (obj instanceof Map && obj['t'] == "FUNC" && obj['ismacro']) {
            return true
        }
    }
    return false
}
def macroexpand(ast, env) {
    while (macro_Q(ast, env)) {
        def mac = Env__get(env, ast[0])
        ast = mac(ast.drop(1))
    }
    return ast
}
def pair_Q(ast) {
    return sequential_Q(ast) && ast.size() > 0
}
def quasiquote(ast) {
    if (! pair_Q(ast)) {
        [Symbol("quote"), ast]
    } else if (ast[0] != null &&
            ast[0]["t"] == "SYMBOL" &&
            ast[0]["value"] == "unquote") {
        ast[1]
    } else if (pair_Q(ast[0]) && ast[0][0]['t'] == "SYMBOL" &&
            ast[0][0]['value'] == "splice-unquote") {
        [Symbol("concat"), ast[0][1], quasiquote(ast.drop(1))]
    } else {
        [Symbol("cons"), quasiquote(ast[0]), quasiquote(ast.drop(1))]
    }
}
def eval_ast(ast, env) {
    switch(ast) {
        case { symbol_Q(it) }:
            return Env__get(env, ast)
        case List:
            return vector_Q(ast) ?
                    vector(ast.collect { EVAL(it,env) }) :
                    ast.collect { EVAL(it,env) }
        case Map:
            def new_hm = [:]
            ast.each { k,v ->
                new_hm[EVAL(k, env)] = EVAL(v, env)
            }
            return new_hm
        default:
            return ast
    }
}

def EVAL(ast, env) {
    while (true) {
        if (! list_Q(ast)) {
            def result = eval_ast(ast, env)
            return result
        }

        ast = macroexpand(ast, env)
        if (! list_Q(ast)) {
            def result = eval_ast(ast, env)
            return result
        }
        if (ast.size() == 0) {
            return ast
        }
        switch (ast[0]) {
//            case { symbol_Q(it) && it['value'] == "eval" }:
//                def rootEnv = Env__getRoot(env)
//                return EVAL(ast[1], rootEnv)
//                break
            case { symbol_Q(it) && it['value'] == "def" }:
                def assignResult = EVAL(ast[2], env)

                def setResult = Env__set(env, ast[1], assignResult)

                return setResult
            case { symbol_Q(it) && it['value'] == "let" }:
                def let_env = Env(env)
                for (int i=0; i < ast[1].size(); i += 2) {
                    Env__set(let_env, ast[1][i], EVAL(ast[1][i+1], let_env))
                }
                env = let_env
                ast = ast[2]
                break // TCO
            case { symbol_Q(it) && it['value'] == "subscribe" }:
                def devicesList = EVAL(ast[1], env)
                def eventName = EVAL(ast[2], env)
                def handler = EVAL(ast[3], env)

                if(device_Q(devicesList)) {
                    devicesList = [devicesList]
                }

                devicesList = devicesList.collect {DeviceReference__get(it)}

                def sub = [
                        devs: devicesList.collect({ it.id }),
                        ev: eventName,
                        fn: handler
                ]

                mlog("subev call", "info", [
                            size: toJson(sub).length(),
                            sub: sub,
                        ])

                state.subs.push(sub)

                subscribe(devicesList, eventName, "eventHandler")
                return null
                break
            case { symbol_Q(it) && it['value'] == "quote" }:
                return ast[1]
            case { symbol_Q(it) && it['value'] == "quasiquote" }:
                ast = quasiquote(ast[1])
                break // TCO
            case { symbol_Q(it) && it['value'] == "defmacro" }:
                def f = EVAL(ast[2], env)
                f.ismacro = true
                return Env__set(env, ast[1], f)
            case { symbol_Q(it) && it['value'] == "macroexpand" }:
                def result = macroexpand(ast[1], env)
                return result
            case { symbol_Q(it) && it['value'] == "try" }:
                def result = EVAL(ast[1], env)
                if(result instanceof Map && result.get("t", "") == "EXCEPTION") {
                    if (ast.size() > 2 &&
                            symbol_Q(ast[2][0]) &&
                            ast[2][0]['value'] == "catch") {
                        def e = null
                        e = result['message']
                        def catchresult = EVAL(ast[2][2], Env(env, [ast[2][1]], [e]))
                        return catchresult
                    } else {
                        return result
                    }
                } else {
                    return result
                }
            case { symbol_Q(it) && it['value'] == "do" }:
                ast.size() > 2 ? eval_ast(ast[1..-2], env) : null
                ast = ast[-1]
                break // TCO
            case { symbol_Q(it) && it['value'] == "if" }:
                def cond = EVAL(ast[1], env)
                if (cond == false || cond == null) {
                    if (ast.size > 3) {
                        ast = ast[3]
                        break // TCO
                    } else {
                        return null
                    }
                } else {
                    ast = ast[2]
                    break // TCO
                }
            case { symbol_Q(it) && it['value'] == "fn" }:
                def result = Function(EVAL, ast[2], env, ast[1])
                return result
            default:
                def el = eval_ast(ast, env)
                def f = el[0]
                def args = el.drop(1)

                if (function_Q(f)) {
                    env = Env(f['env'], f['params'], args)
                    ast = f['ast']
                    break // TCO
                } else if (globalClosure_Q(f)) {
                    def result = GlobalClosureReference__call(f, args)
                    return result
                } else if (f instanceof Closure) {
                    def result =  f(args)
                    return result
                }
                return f
        }
    }
}

def PRINT(exp) {
    return exp
}

def REP(str, e) {
    def readResult = READ(str)
    mlog("READ", "debug", readResult)
    def evalResult = EVAL(readResult, e)
    mlog("EVAL", "debug", evalResult)

    def result = PRINT(evalResult)
    return result
}

def REP_ALL(str, e) {
    READ("[$str]").collect {
        mlog("EVALEXPR", "debug", it)

        def result = EVAL(it, e)
        def outputResult = PRINT(result)
        mlog("PRINT", "debug", outputResult)
        return outputResult
    }
}

def interpret(str, e) {
    mlog("Interpreting ${str}", "debug")
    try {
        REP("(def not (fn* (a) (if a false true)))", e)
        //REP("(def! load-file (fn* (f) (eval (read-string (str \"(do \" (slurp f) \")\")))))")
        REP("(defmacro! cond (fn (& xs) (if (> (count xs) 0)" +
                " (list 'if (first xs) (if (> (count xs) 1) (nth xs 1)" +
                " (throw \"odd number of forms to cond\"))" +
                " (cons 'cond (rest (rest xs)))))))", e)
        REP("(def *gensym-counter* (atom 0))", e)
        REP("(def gensym (fn [] (symbol (str \"G__\" (swap *gensym-counter* (fn* [x] (+ 1 x)))))))", e)
        REP("(defmacro or (fn (& xs) " +
                "(if (empty? xs) " +
                "nil " +
                "(if (= 1 (count xs)) " +
                "(first xs) " +
                "(let (condvar (gensym)) " +
                "`(let (~condvar ~(first xs)) " +
                "(if ~condvar ~condvar (or ~@(rest xs)))))))))", e)


        def res = REP_ALL(str, e)
        return res
    } catch(exc) {
        return MyreLispException(exc)
    }
}
///// END interpreter implementation
def halt() {
    log.info("Halting.")
    unsubscribe()
    state.subs = []
    state.active = false
    return true
}

def resume() {
    log.info("Resuming.")
    state.active = true

    def result = execute()
    log.info("Result: ${result}")
    return result
}

def execute() {
    mlog("Received request to execute. Active: ${state.active}")
    unsubscribe()
    if(state.active == true) {
        state.subs = []
        state.traces = []
        def res = interpret(state.src, Env())
        state.traces = []
        return [
                result: res,
        ]
    }
}

def eventHandler(event) {
    if(state.active == true) {
        def deviceId = event.deviceId
        def eventName = event.name

        // TODO: handle myre events with no device ID

        def validSubs = state.subs.findAll({ s -> deviceId in s["devs"] && eventName == s["ev"] })
        validSubs.each({
            def decoded = it["fn"]

            mlog("Found event subscription for event $deviceId :: $eventName", "info", decoded)

            // decoded should now be a Function map
            Function__call(decoded, args)
        })
    }
}

Boolean setup(projectId, name, description, source) {
    log.info("Setting up ${projectId} ${name} ${description} ${source}")

    if(projectId && name && source) {
        state.projectId = projectId
        state.name = name
        state.desc = description
        state.src = source
        state.traces = []

        state.inited = true

        return true
    } else {
        return false
    }
}

Boolean updateProject(name, description, source) {
    if(name && source) {
        unsubscribe()
        state.subs = []

        state.modified = now()
        state.name = name
        state.desc = description
        state.src = source
    }
}

def installed() {
    state.created = now()
    state.modified = now()
    state.active = true
    //interpret(state.src)

    return true
}

def getSummary() {
    [
            appId: app.id,
            projectId: state.projectId,
            name: state.name,
            description: state.desc,
            createdAt: state.created,
            modifiedAt: state.modified,
            source: state.src,
            running: state.active
    ]
}

def getSource() {
    return state.src
}

def getProjectId() {
    return state.projectId
}

def getCreatedAt() {
    return state.created
}
