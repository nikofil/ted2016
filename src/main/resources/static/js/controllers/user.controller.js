app.controller('UserController', function($scope, $http, $state, AuthService) {
    if (AuthService.user.jwt) {
        $state.go('main.store');
    }

    var marker = null;
    $scope.markers = {};

    $scope.center = {
        autoDiscover: true
    };

    $scope.$on('leafletDirectiveMap.map.click', function (_, event) {
        marker = {
            lat: event.leafletEvent.latlng.lat,
            lng: event.leafletEvent.latlng.lng,
            draggable: true,
            focus: true
        };
        $scope.markers = {marker: marker};
    });

    $scope.$on('leafletDirectiveMarker.map.click', function () {
        marker = null;
        $scope.markers = {};
    });

    $scope.$on('leafletDirectiveMarker.map.dragend', function(_, event) {
        marker.lat = event.leafletEvent.target._latlng.lat;
        marker.lng = event.leafletEvent.target._latlng.lng;
    });

    $scope.invalid = function(field) {
        return field.$touched && field.$invalid;
    };

    $scope.doLogin = function() {
        AuthService.login($scope.login).then(function() {
        	$scope.loginError = null;
        	$state.go('main.store');
        }, function(error) {
        	$scope.loginError = error;
        });
    };

    $scope.doRegister = function(form) {
    	$scope.pwNoMatch = false;
        $scope.registerSuccess = false;
        $scope.registerError = null;

        if ($scope.register.password != $scope.register.password2) {
            $scope.pwNoMatch = true;
            $('#rpassword').focus();
        } else {
            if (marker) {
                $scope.register.latitude = marker.lat % 360;
                if ($scope.register.latitude > 180) {
                    $scope.register.latitude -= 360;
                }
                $scope.register.longitude = marker.lng % 360;
                if ($scope.register.longitude > 180) {
                    $scope.register.longitude -= 360;
                }
            }
        	AuthService.register($scope.register).then(function() {
        		$scope.registerError = null;
        		$scope.registerSuccess = true;
        	}, function(error) {
        		$scope.registerError = error;
        	});
        }
    };
});