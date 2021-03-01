module.exports = function(grunt) {
    grunt.initConfig({
        qunit: {
            src: ['qunit/test.html'],
            force: true
        },
        uglify: {
            options: {
                banner: '/*! venus <%= grunt.template.today("dd-mm-yyyy") %> */\n'
            },
            venus: {
                files: {
                    'out/js/venus.js': ['build/kotlin-js-min/main/venus.js'],
                    'out/js/kotlin.js': ['build/kotlin-js-min/main/kotlin.js'],
                    'out/js/codemirror/codemirror.js': ['src/main/frontend/js/codemirror/*.js']
                }
            }
        },
        cssmin: {
            venus: {
                files: [{
                    'out/css/venus.css': ['src/main/frontend/css/*.css', "!src/main/frontend/css/alertify/*", "!src/main/frontend/css/themes/*"],
                },
                {
                    expand: true,
                    cwd: 'src/main/frontend/css/themes/',
                    src: ['*.css'],
                    dest: 'out/css/themes/',
                }
                ]
            }
        },

        copy: {
		  	venus: {
			    files: [
                    {expand: true,
                        cwd: 'src/main/frontend/images',
                        src: '**',
                        dest: 'out/images/',},
                    {expand: true,
                        cwd: 'src/main/frontend/scripts',
                        src: '**',
                        dest: 'out/scripts/',},
                    {expand: true,
                        cwd: 'src/main/frontend/packages',
                        src: '**',
                        dest: 'out/packages/',},
                    {expand: true,
                        cwd: 'src/main/frontend/css/alertify',
                        src: '**',
                        dest: 'out/css/alertify/',},
                    {src: 'src/main/frontend/CNAME',
                        dest: 'out/CNAME',},
                ]
		  	},
            jvm: {
                files: [
                    {expand: true,
                        cwd: 'build/libs',
                        dest: 'out/jvm/',
                        src: [
                            'venus*.jar'
                        ],
                        rename: function(dest, src) {
                            return dest + "venus-jvm-latest.jar"
                        }
                    },
                ]
            },
		},

        htmlmin: {
            venus: {
                options: {
                    removeComments: true,
                    collapseWhitespace: true,
                    removeEmptyAttributes: true,
                    removeCommentsFromCDATA: true,
                    removeRedundantAttributes: true,
                    collapseBooleanAttributes: true
                },
                files: {
                    'out/index.html': ['src/main/frontend/index.html']
                }
            }
        },
    });
    grunt.loadNpmTasks('grunt-contrib-qunit');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-contrib-htmlmin');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.registerTask('test', 'qunit:src');
    grunt.registerTask('dist', ['uglify:venus', 'cssmin', 'htmlmin', 'copy:venus']);
    grunt.registerTask('frontend', ['cssmin:venus', 'htmlmin:venus']);
    grunt.registerTask('distjvm', ['copy:jvm']);
};
