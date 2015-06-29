/* global angular */

angular.module('angular-hal', []).service('halClient', ['$http', '$q', '$window',

    function ($http, $q, $window) {

        var UriTemplate = $window.uriTemplate.template;

        this.$get = function (href, options) {
            return callService('GET', href, options);
        }; //get

        this.$post = function (href, options, data) {
            return callService('POST', href, options, data);
        }; //post

        this.$put = function (href, options, data) {
            return callService('PUT', href, options, data);
        }; //put

        this.$patch = function (href, options, data) {
            return callService('PATCH', href, options, data);
        }; //patch

        this.$delete = function (href, options) {
            return callService('DELETE', href, options);
        }; //del


        function Resource(href, options, data) {
            var linksAttribute = options.linksAttribute || '_links';
            var embeddedAttribute = options.embeddedAttribute || '_embedded';
            var ignoreAttributePrefixes = options.ignoreAttributePrefixes || ['_', '$'];
            var links = {};
            var embedded = {};

            href = getSelfLink(href, data).href;

            defineHiddenProperty(this, '$href', function (rel, params) {
                if (!(rel in links)) return null;

                return hrefLink(links[rel], params);
            });
            defineHiddenProperty(this, '$has', function (rel) {
                return rel in links;
            });
            defineHiddenProperty(this, '$get', function (rel, params) {
                var link = links[rel];
                return callLink('GET', link, params);
            });
            defineHiddenProperty(this, '$post', function (rel, params, data) {
                var link = links[rel];
                return callLink('POST', link, params, data);
            });
            defineHiddenProperty(this, '$put', function (rel, params, data) {
                var link = links[rel];
                return callLink('PUT', link, params, data);
            });
            defineHiddenProperty(this, '$patch', function (rel, params, data) {
                var link = links[rel];
                return callLink('PATCH', link, params, data);
            });
            defineHiddenProperty(this, '$delete', function (rel, params) {
                var link = links[rel];
                return callLink('DELETE', link, params);
            });


            Object.keys(data)
                .filter(function (key) {
                    return key !== linksAttribute &&
                        key !== embeddedAttribute &&
                        (!~ignoreAttributePrefixes.indexOf(key[0]));
                })
                .forEach(function (key) {
                    Object.defineProperty(this, key, {
                        configurable: false,
                        enumerable: true,
                        value: data[key]
                    });
                }, this);


            if (data[linksAttribute]) {
                Object
                    .keys(data[linksAttribute])
                    .forEach(function (rel) {
                        var link = data[linksAttribute][rel];
                        link = normalizeLink(href, link);
                        links[rel] = link;
                    }, this);
            }

            if (data[embeddedAttribute]) {
                Object
                    .keys(data[embeddedAttribute])
                    .forEach(function (rel) {
                        var embedded = data[embeddedAttribute][rel];
                        var link = getSelfLink(href, embedded);
                        links[rel] = link;
                        //console.log(link)

                        var resource = createResource(href, options, embedded);

                        embedResource(resource);

                    }, this);
            }

            function defineHiddenProperty(target, name, value) {
                Object.defineProperty(target, name, {
                    configurable: false,
                    enumerable: false,
                    writable: false,
                    value: value
                });
            } //defineHiddenProperty


            function embedResource(resource) {
                if (Array.isArray(resource)) return resource.map(function (resource) {
                    return embedResource(resource);
                });

                var href = resource.$href('self');

                embedded[href] = $q.when(resource);
            } //embedResource

            function hrefLink(link, params) {
                var href = link.templated ? new UriTemplate(link.href).stringify(params) : link.href;

                return href;
            } //hrefLink

            function callLink(method, link, params, data) {
                var linkHref;

                if (Array.isArray(link)) {
                    return $q.all(link.map(function (link) {
                        if (method !== 'GET') throw 'method is not supported for arrays';

                        return callLink(method, link, params, data);
                    }));
                }

                linkHref = hrefLink(link, params);

                if (method === 'GET') {
                    if (linkHref in embedded) return embedded[linkHref];

                    return callService(method, linkHref, options, data);
                } else {
                    return callService(method, linkHref, options, data);
                }

            } //callLink

            function getSelfLink(baseHref, resource) {

                if (Array.isArray(resource)) return resource.map(function (resource) {
                    return getSelfLink(baseHref, resource);
                });

                return normalizeLink(baseHref, resource && resource[linksAttribute] && resource[linksAttribute].self);
            } //getSelfLink

        } //Resource


        function createResource(href, options, data) {
            if (Array.isArray(data)) return data.map(function (data) {
                return createResource(href, options, data);
            });

            var resource = new Resource(href, options, data);

            return resource;

        } //createResource


        function normalizeLink(baseHref, link) {
            if (Array.isArray(link)) return link.map(function (link) {
                return normalizeLink(baseHref, link);
            });

            if (link) {
                if (typeof link === 'string') link = {
                    href: link
                };
                link.href = resolveUrl(baseHref, link.href);
            } else {
                link = {
                    href: baseHref
                };
            }

            return link;
        } //normalizeLink


        function callService(method, href, options, data) {
            if (!options) options = {};
            if (!options.headers) options.headers = {};
            if (!options.headers['Content-Type']) options.headers['Content-Type'] = 'application/json';
            if (!options.headers.Accept) options.headers.Accept = 'application/hal+json,application/json';

            var resource = (
                $http({
                    method: method,
                    url: options.transformUrl ? options.transformUrl(href) : href,
                    headers: options.headers,
                    data: data
                })
                .then(function (res) {

                    switch (Math.floor(res.status / 100)) {
                    case 2:
                        if (res.data) return createResource(href, options, res.data);
                        if (res.headers('Content-Location')) return res.headers('Content-Location');
                        if (res.headers('Location')) return res.headers('Location');
                        return null;

                    default:
                        return $q.reject(res.status);
                    }
                })
            );

            return resource;
        } //callService


        function resolveUrl(baseHref, href) {
            var resultHref = '';
            var reFullUrl = /^((?:\w+\:)?)((?:\/\/)?)([^\/]*)((?:\/.*)?)$/;
            var baseHrefMatch = reFullUrl.exec(baseHref);
            var hrefMatch = reFullUrl.exec(href);

            for (var partIndex = 1; partIndex < 5; partIndex++) {
                if (hrefMatch[partIndex]) resultHref += hrefMatch[partIndex];
                else resultHref += baseHrefMatch[partIndex];
            }

            return resultHref;
        } //resolveUrl

    }

]); //service

uriTemplate = (function()
{
    var operatorOptions = {
        "": {
            "prefix": "",
            "seperator": ",",
            "assignment": false,
            "assignEmpty": false,
            "encode": percentEncode
        },
        "+": {
            "prefix": "",
            "seperator": ",",
            "assignment": false,
            "assignEmpty": false,
            "encode": encodeURI
        },
        "#": {
            "prefix": "#",
            "seperator": ",",
            "assignment": false,
            "assignEmpty": false,
            "encode": encodeURI
        },
        ".": {
            "prefix": ".",
            "seperator": ".",
            "assignment": false,
            "assignEmpty": false,
            "encode": percentEncode
        },
        "/": {
            "prefix": "/",
            "seperator": "/",
            "assignment": false,
            "assignEmpty": false,
            "encode": encodeURIComponent
        },
        ";": {
            "prefix": ";",
            "seperator": ";",
            "assignment": true,
            "assignEmpty": false,
            "encode": encodeURIComponent
        },
        "?": {
            "prefix": "?",
            "seperator": "&",
            "assignment": true,
            "assignEmpty": true,
            "encode": encodeURIComponent
        },
        "&": {
            "prefix": "&",
            "seperator": "&",
            "assignment": true,
            "assignEmpty": true,
            "encode": encodeURIComponent
        }
    }; //operatorOptions

    function percentEncode(value)
    {
        /*
         http://tools.ietf.org/html/rfc3986#section-2.3
         */
        var unreserved = "-._~";

        if (isUndefined(value)) return '';

        value = value.toString();

        return Array.prototype.map.call(value, function (ch)
        {
            var charCode = ch.charCodeAt(0);

            if (charCode >= 0x30 && charCode <= 0x39) return ch;
            if (charCode >= 0x41 && charCode <= 0x5a) return ch;
            if (charCode >= 0x61 && charCode <= 0x7a) return ch;

            if (~unreserved.indexOf(ch)) return ch;

            return '%' + charCode.toString(16).toUpperCase();
        }).join('');

    } //percentEncode

    function isDefined(value)
    {
        return !isUndefined(value);
    } //isDefined
    function isUndefined(value)
    {
        /*
         http://tools.ietf.org/html/rfc6570#section-2.3
         */
        if (value === null) return true;
        if (value === undefined) return true;
        if (Array.isArray(value)) {
            if (value.length === 0) return true;
        }

        return false;
    } //isUndefined


    function UriTemplate(template)
    {
        /*
         http://tools.ietf.org/html/rfc6570#section-2.2

         expression    =  "{" [ operator ] variable-list "}"
         operator      =  op-level2 / op-level3 / op-reserve
         op-level2     =  "+" / "#"
         op-level3     =  "." / "/" / ";" / "?" / "&"
         op-reserve    =  "=" / "," / "!" / "@" / "|"
         */
        var reTemplate = /\{([\+#\.\/;\?&=\,!@\|]?)([A-Za-z0-9_\,\.\:\*]+?)\}/g;
        var reVariable = /^([\$_a-z][\$_a-z0-9]*)((?:\:[1-9][0-9]?[0-9]?[0-9]?)?)(\*?)$/i;
        var match;
        var pieces = [];
        var glues = [];
        var offset = 0;
        var pieceCount = 0;

        while (!!(match = reTemplate.exec(template))) {
            glues.push(template.substring(offset, match.index));
            /*
             The operator characters equals ("="), comma (","), exclamation ("!"),
             at sign ("@"), and pipe ("|") are reserved for future extensions.
             */
            if (match[1] && ~'=,!@|'.indexOf(match[1])) {
                throw new Error("operator '" + match[1] + "' is reserved for future extensions");
            }

            offset = match.index;
            pieces.push({
                operator: match[1],
                variables: match[2].split(',').map(variableMapper)
            });
            offset += match[0].length;
            pieceCount++;
        }

        function variableMapper(variable)
        {
            var match = reVariable.exec(variable);
            return {
                name: match[1],
                maxLength: match[2] && parseInt(match[2].substring(1), 10),
                composite: !!match[3]
            };
        }

        glues.push(template.substring(offset));


        this.stringify = function (data)
        {
            var str = '';
            data = data || {};

            str += glues[0];
            if (!pieces.every(function (piece, pieceIndex)
                {

                    var options = operatorOptions[piece.operator];
                    var parts;

                    parts = piece.variables.map(function (variable)
                    {
                        var value = data[variable.name];

                        if (!Array.isArray(value)) value = [value];

                        value = value.filter(isDefined);

                        if (isUndefined(value)) return null;

                        if (variable.composite) {
                            value = value.map(function (value)
                            {

                                if (typeof value === 'object') {

                                    value = Object.keys(value).map(function (key)
                                    {
                                        var keyValue = value[key];
                                        if (variable.maxLength) keyValue = keyValue.substring(0, variable.maxLength);

                                        keyValue = options.encode(keyValue);

                                        if (keyValue) keyValue = key + '=' + keyValue;
                                        else {
                                            keyValue = key;
                                            if (options.assignEmpty) keyValue += '=';
                                        }

                                        return keyValue;
                                    }).join(options.seperator);

                                } else {
                                    if (variable.maxLength) value = value.substring(0, variable.maxLength);

                                    value = options.encode(value);

                                    if (options.assignment) {
                                        if (value) value = variable.name + '=' + value;
                                        else {
                                            value = variable.name;
                                            if (options.assignEmpty) value += '=';
                                        }
                                    }
                                }

                                return value;
                            });

                            value = value.join(options.seperator);
                        } else {
                            value = value.map(function (value)
                            {
                                if (typeof value === 'object') {
                                    return Object.keys(value).map(function (key)
                                    {
                                        var keyValue = value[key];
                                        if (variable.maxLength) keyValue = keyValue.substring(0, variable.maxLength);
                                        return key + ',' + options.encode(keyValue);
                                    }).join(',');
                                } else {
                                    if (variable.maxLength) value = value.substring(0, variable.maxLength);

                                    return options.encode(value);
                                }

                            });
                            value = value.join(',');

                            if (options.assignment) {
                                if (value) value = variable.name + '=' + value;
                                else {
                                    value = variable.name;
                                    if (options.assignEmpty) value += '=';
                                }
                            }

                        }

                        return value;
                    });

                    parts = parts.filter(isDefined);
                    if (isDefined(parts)) {
                        str += options.prefix;
                        str += parts.join(options.seperator);
                    }

                    str += glues[pieceIndex + 1];
                    return true;
                })) return false;

            return str;
        }; //stringify

    } //UriTemplate

    return {
        template: UriTemplate
    };

})();