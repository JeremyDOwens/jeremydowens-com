
{
    const authUser = () => new Promise((resolve, reject) => {
        const email = $('#login_email').val();
        const password = $('#login_password').val();

        if (email && password) {
            $.ajax({
                type: 'POST',
                url: '/authenticate',
                data: JSON.stringify({
                    email,
                    password,
                }),
                contentType: 'application/json',
                success: (s) => {
                    if (s.success) {
                        $('#loginmessage').text(s.success);
                        $('.spinner').hide();
                        window.location.replace('/');
                    }
                    if (s.error) {
                        $('#loginmessage').text(`ERROR: ${s.error}`);
                        $('.spinner').hide();
                    }
                    resolve(s);
                },
                error: (e) => {
                    reject(e);
                },
            });
        } else {
            $('.spinner').hide();
            $('#loginmessage').text('ERROR: Invalid email or password.');
            reject(Error('Invalid arguments.'));
        }
    });

    const createUser = () => new Promise((resolve, reject) => {
        const email = $('#create_acct_email').val();
        const uname = $('#create_acct_uname').val()
        if (email && uname) {
            $.ajax({
                type: 'POST',
                url: '/createaccount',
                data: JSON.stringify({
                    email,
                    uname
                }),
                contentType: 'application/json',
                success: (s) => {
                    if (s.success) { $('#resultmessage').text(s.success); }
                    if (s.error) { $('#resultmessage').text(`ERROR: ${s.error}`); }
                    resolve(s);
                },
                error: (e) => {
                    reject(e);
                },
            });
        } else {
            $('.spinner').hide();
            $('#resultmessage').text('ERROR: Invalid email.');
            reject(Error('Invalid arguments.'));
        }
    });

    const changePassword = () => new Promise((resolve, reject) => {
        const oldpw = $('#old_password').val();
        const newpw = $('#new_password').val();
        if (oldpw && newpw) {
            $.ajax({
                type: 'POST',
                url: '/changepassword',
                data: JSON.stringify({
                    oldpw,
                    newpw,
                }),
                contentType: 'application/json',
                success: (s) => {
                    if (s.success) $('#changemessage').text(s.success);
                    else if (s.error) $('#changemessage').text(`ERROR: ${s.error}`);
                    resolve(s);
                },
                error: (e) => {
                    reject(e);
                },
            });
        } else {
            $('.spinner').hide();
            $('#changemessage').text('ERROR: Invalid arguments.');
            reject(Error('Invalid arguments.'));
        }
    });

    const recoverPassword = () => new Promise((resolve, reject) => {
        const email = $('#recovery_email').val();

        if (email) {
            $.ajax({
                type: 'POST',
                url: '/getnewpassword',
                data: JSON.stringify({
                    email,
                }),
                contentType: 'application/json',
                success: (s) => {
                    if (s.success) $('#recovermessage').text(s.success);
                    else if (s.error) $('#recovermessage').text(`ERROR: ${s.error}`);
                    resolve(s);
                },
                error: (e) => {
                    reject(e);
                },
            });
        } else {
            $('.spinner').hide();
            $('#recovermessage').text('ERROR: Invalid email.');
            reject(Error('Invalid arguments.'));
        }
    });

    $(document).ready(function () {
        $('#acctcreate').on('submit', function (e) {
            e.preventDefault();
            $('#create_spinner').show();
            createUser().then(() => {
                $('.spinner').hide();
            });
        });
        $('#acctlogin').on('submit', function (e) {
            e.preventDefault();
            $('#login_spinner').show();
            authUser().then(() => {
                $('.spinner').hide();
            });
        });
        $('#get_new_password').on('submit', function (e) {
            e.preventDefault();
            $('#recover_spinner').show();
            recoverPassword().then(() => {
                $('.spinner').hide();
            });
        });
        $('#change_password').on('submit', function (e) {
            e.preventDefault();
            $('#change_spinner').show();
            changePassword().then(() => {
                $('.spinner').hide();
            });
        });
    });
}