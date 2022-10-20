yieldUnescaped '<!DOCTYPE html>'
html(lang: 'en') {
  head {
      title 'Login Page'
  }
  body {
    if (userprofile.isLoggedIn()) {
      h2 "Logged in as ${userprofile.uid}!"
    } else {
      h2 'Hello, please log in:'
      br
      br
      form(action: '/demo/auth/form/success.gtpl', method: 'post') {
        p {
          strong 'Please Enter Your User Name: '
        }
        input(type: 'text', name: 'j_username', size: 25)
        p {}
        p {}
        input(type: 'password', name: 'j_password', size: 15)
        p {}
        p {}
        input(type: 'submit', value: 'Submit')
        input(type: 'reset', value: 'Reset')
      }
      p "Original path: ${originalPath}"
    }
  }
}
