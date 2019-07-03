window.setInterval(function () {
    $.ajax(
        {
            url: '/api/get-tasks',
            type: 'GET',
            success: function (data, textStatus, jqXHR) {
                var response = JSON.parse(jqXHR.responseText);

                var bodyHtml = "";

                for (var currentBackupTask = 0; currentBackupTask < response.length; currentBackupTask++) {
                    var backupTask = response[currentBackupTask];

                    bodyHtml +=
                        '<tr>\n' +
                        '<th scope="row">' + backupTask.type + '</th>\n' +
                        '<td>' + backupTask.state + '</td>\n' +
                        '<td>' + backupTask.error + '</td>\n' +
                        '<td>' + backupTask.interrupted + '</td>\n' +
                        '<td class="tableColumn-time">\n' +
                        '    <div>' + backupTask.time + '</div>\n' +
                        '    <div class="d-flex flex-row-reverse bd-highlight">\n' +
                        '        <form action="/cancel-task" method="post">\n' +
                        '            <input hidden name="taskId" value="' + backupTask.id + '">\n' +
                        '            <button class="btn-sm btn-info innerTableColumnButton" type="submit">\n' +
                        '                Cancel\n' +
                        '            </button>\n' +
                        '        </form>\n' +
                        '    </div>\n' +
                        '</td>' +
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