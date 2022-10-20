yieldUnescaped '<!DOCTYPE html>'
html(lang:'en') {
  head {
    title('500 - Server error')
    meta(charset: 'utf-8')
    meta(name: 'viewport', content: 'width=device-width, initial-scale=1.0')
    link(rel: 'stylesheet', href: bootstrapCss())
  }
  body {
    div(class: 'container') {
      h1('Server error')
      p(class: 'exception') {
        yield "Exception ${stringOf { _throwable } }"
      }
      p(class: 'exceptionMessage') {
        yield "Exception message ${stringOf { _message } }"
      }
      pre {
        code(class: 'trace') {
          StringWriter s = new StringWriter()
          if (_throwable) {
            org.codehaus.groovy.runtime.StackTraceUtils.printSanitizedStackTrace(_throwable, new PrintWriter(s))
          }
          yield "${s.toString()}"
        }
      }
    }
  }
}
