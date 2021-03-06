app.controller('CategoryController', function ($scope, $http, $stateParams, $state, $timeout, AuthService) {
    $scope.maxSize = 5;  // pagination size
    $scope.itemsPerPage = 10;
    $scope.currentPage = 1;
    $scope.totalItems =  0;

    $scope.loggedIn = AuthService.isLoggedIn();
    $scope.category = { name: $stateParams.categoryName };
    $scope.items = [];
    $scope.submenuSize = 5;

    $http.get('api/categories/' + $stateParams.categoryId).then(function(response) {
        $scope.getItems();
        $scope.category = response.data;
        $scope.breadcrumb = [];
        var parent = $scope.category.parent;
        while (parent != null) {
            $scope.breadcrumb.unshift(parent);
            parent = parent.parent;
        }
    }, function() {
        $state.go('page_not_found');
    });

    $scope.getItems = function() {
        $http.get('api/categories/' + $stateParams.categoryId + '/items', {
            params: {
                page: $scope.currentPage - 1,
                size: $scope.itemsPerPage
            }
        }).then(function success(response) {
            $scope.items = response.data.content.map(function (item) {
                if (item.currentbid) {
                    item.currentbid = +item.currentbid / 100;
                }
                if (item.buyprice) {
                    item.buyprice = +item.buyprice / 100;
                }

                if (!item.finished) {
                    item.finished = new Date() > new Date(item.endDate);
                }
                item.end = (item.finished ? "Closed" : "Ends:");
                item.endOffset = moment(item.endDate).fromNow();
                return item;
            });
            $scope.totalItems = response.data.totalElements;

        });
    };

    $scope.needPagination = function() {
        return $scope.totalItems > $scope.itemsPerPage;
    };

    $scope.doSearch = function (name) {
        if (name) {
            $state.go('main.search', {name: name, categoryId: $stateParams.categoryId});
        } else {
            $('#search').popover('show');
            $timeout(function() {$('#search').popover('hide');}, 2000);
        }
    };

    $(function() {
        $('#search').popover({
            trigger: 'manual',
            placement: 'bottom'
        });
    });
});
