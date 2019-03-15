$('#signInButton').click(function (e) {
    e.preventDefault();

    var login = $('#inputLogin').val();
    var password = $('#inputPassword').val();

    var credentials = {login: login, password: password};
    var jsonCredentials = JSON.stringify(credentials);

    $.ajax(
        {
            url: '/api/login',
            type: 'POST',
            contentType: 'application/json',
            data: jsonCredentials,
            success: function () {
                localStorage.setItem('login', login);
                window.location.replace('/dashboard');
            },
            error: function (jqXHR) {
                var response = JSON.parse(jqXHR.responseText);
                alert(response.error)
            }
        }
    );

    return false;
});