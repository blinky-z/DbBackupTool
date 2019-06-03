window.setInterval(function () {
    $.ajax(
        {
            url: '/api/get-states',
            type: 'GET',
            success: function (data, textStatus, jqXHR) {
                var response = JSON.parse(jqXHR.responseText);

                var bodyHtml = "";

                for (var currentBackupTask = 0; currentBackupTask < response.length; currentBackupTask++) {
                    var backupTask = response[currentBackupTask];

                    bodyHtml += '<tr>\n' +
                        '<th scope="row">' + backupTask.type + '</th>\n' +
                        '<td>' + backupTask.state + '</td>\n' +
                        '<td>' + backupTask.time + '</td>\n' +
                        '</tr>\n';
                }

                if (bodyHtml !== "") {
                    var backupTasksTableBody = $('#backupTasksTableBody');
                    backupTasksTableBody.html(bodyHtml);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.log("Error updating backup states. Error code: " + jqXHR.status);
            }
        }
    );

}, 5000);