/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

var template = require('./cam-cockpit-form.html?raw');

const Form = require('@bpmn-io/form-js').Form;

module.exports = [
  function() {
    return {
      restrict: 'A',

      scope: {
        name: '=',
        source: '='
      },

      template: template,

      link: function($scope, $element) {
        $scope.error = null;

        async function createForm() {
          const json = JSON.parse($scope.source);
          const form = new Form({
            container: $element.find('.operatonForm')[0]
          });
          await form.importSchema(json);
          form.setProperty('readOnly', true);
        }

        createForm().catch(e => {
          $scope.error = e;
        });
      }
    };
  }
];
