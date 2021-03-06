<%@tag description="Confirmation Dialog" pageEncoding="UTF-8"%>

<%@attribute name="id" required="true"%>
<%@attribute name="title" required="true"%>
<%@attribute name="actionUrl" required="true"%>
<%@attribute name="actionText" required="true"%>

<div id="${id}" class="modal">
    <div class="modal-dialog">
        <div class="modal-content">
            <form id="role-form" method="POST" action="${actionUrl}">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">x</button>
                    <h4 class="modal-title">${title}</h4>
                </div>

                <div class="modal-body">
                    <jsp:doBody/>
                </div>

                <div class="modal-footer">
                    <button type="submit" class="btn btn-primary">${actionText}</button>
                    <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                </div>
            </form>
        </div>
    </div>
</div>