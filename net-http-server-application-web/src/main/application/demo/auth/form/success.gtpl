yieldUnescaped '<!DOCTYPE html>'
html(lang: 'en') {
  head {
     title 'Login OK'
  }
  body {
     h1 'Welcome'
     div {
        yield "userprofile=${userprofile}"
     }
  }
}
